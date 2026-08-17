package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.repository.*;
import uz.infosec.risk.web.dto.AnalyticsDtos.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Read-only aggregates for the risk matrix and the dashboard.
 *
 * <p>Everything here reads the pre-computed snapshot columns, so these are
 * plain GROUP BY queries - no formulas are re-run at read time.
 */
@Service
public class AnalyticsService {

    /** A measure with this status is finished and can never be overdue. */
    private static final String STATUS_DONE = "Выполнено";

    /** Shared zero pair for months with no deadlines; never mutated. */
    private static final long[] EMPTY_MONTH = {0, 0};

    private final RiskRepository riskRepository;
    private final RiskControlRepository riskControlRepository;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final ControlRepository controlRepository;
    private final RiskCalculationService calculator;

    public AnalyticsService(RiskRepository riskRepository,
                            RiskControlRepository riskControlRepository,
                            AssetRepository assetRepository,
                            ThreatRepository threatRepository,
                            ControlRepository controlRepository,
                            RiskCalculationService calculator) {
        this.riskRepository = riskRepository;
        this.riskControlRepository = riskControlRepository;
        this.assetRepository = assetRepository;
        this.threatRepository = threatRepository;
        this.controlRepository = controlRepository;
        this.calculator = calculator;
    }

    /**
     * The 5x5 heat map. Rows are asset criticality 5..1 and columns are threat
     * level 1..5, exactly the layout of the Матрица рисков sheet.
     */
    @Transactional(readOnly = true)
    public RiskMatrixResponse riskMatrix() {
        // One grouped query, then fill the grid in memory. 25 cells is far
        // cheaper to complete here than with 25 separate COUNTIFS.
        Map<String, Long> counts = new HashMap<>();
        int total = 0;
        for (Object[] row : riskRepository.matrixCounts()) {
            int a = ((Number) row[0]).intValue();
            int t = ((Number) row[1]).intValue();
            long n = ((Number) row[2]).longValue();
            counts.put(a + ":" + t, n);
            total += (int) n;
        }

        List<Integer> assetRatings = List.of(5, 4, 3, 2, 1);
        List<Integer> threatRatings = List.of(1, 2, 3, 4, 5);

        List<MatrixCell> cells = new ArrayList<>(25);
        for (int a : assetRatings) {
            for (int t : threatRatings) {
                Long n = counts.get(a + ":" + t);
                RiskLevel level = calculator.classify(a, t);
                // null (not 0) for an empty cell, mirroring the sheet's
                // IF(COUNTIFS(...)=0,"",...).
                cells.add(new MatrixCell(a, t, n == null ? null : n.intValue(),
                        level.getLevel(), level.getLabel()));
            }
        }

        return new RiskMatrixResponse(assetRatings, threatRatings, cells, total,
                // Legend text from the Матрица рисков sheet (I11:P16).
                List.of(new MatrixLegendItem(1, "Juda past", "Очень низкая"),
                        new MatrixLegendItem(2, "Past", "Низкая"),
                        new MatrixLegendItem(3, "O‘rta", "Средняя"),
                        new MatrixLegendItem(4, "Yuqori", "Высокая"),
                        new MatrixLegendItem(5, "Kritik", "Критичная")),
                List.of(new MatrixLegendItem(1, "Ahamiyatsiz", "Незначительный"),
                        new MatrixLegendItem(2, "Past", "Низкий"),
                        new MatrixLegendItem(3, "O‘rta", "Средний"),
                        new MatrixLegendItem(4, "Yuqori", "Высокий"),
                        new MatrixLegendItem(5, "Juda yuqori", "Очень высокий")),
                List.of(new MatrixLegendItem(1, "Ahamiyatsiz", "Незначительный"),
                        new MatrixLegendItem(2, "Past", "Низкий"),
                        new MatrixLegendItem(3, "O‘rta", "Средний"),
                        new MatrixLegendItem(4, "Yuqori", "Высокий"),
                        new MatrixLegendItem(5, "Kritik", "Критический")));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        long implemented = riskControlRepository.countByControlType(ControlType.IMPLEMENTED);
        long planned = riskControlRepository.countByControlType(ControlType.PLANNED);
        long totalLinks = implemented + planned;

        Map<Long, Object[]> summary = new HashMap<>();
        for (Object[] row : riskRepository.assetRiskSummary()) {
            summary.put(((Number) row[0]).longValue(), row);
        }

        List<AssetGauge> gauges = assetRepository.findAll().stream()
                .map(asset -> {
                    Object[] row = summary.get(asset.getId());
                    // An asset with no risks yet still gets a card, showing zero.
                    int riskCount = row == null ? 0 : ((Number) row[1]).intValue();
                    Integer worstCurrent = row == null || row[2] == null
                            ? null : ((Number) row[2]).intValue();
                    Integer worstResidual = row == null || row[3] == null
                            ? null : ((Number) row[3]).intValue();
                    return new AssetGauge(
                            asset.getId(), asset.getCode(), asset.getName(),
                            asset.getCriticality(), asset.getCriticalityRating(),
                            riskCount,
                            worstCurrent, labelOf(worstCurrent),
                            worstResidual, labelOf(worstResidual));
                })
                .sorted(Comparator
                        // Worst first: that is what a manager wants at the top.
                        .comparing((AssetGauge g) -> g.worstCurrentLevel() == null
                                ? 0 : g.worstCurrentLevel(), Comparator.reverseOrder())
                        .thenComparing(AssetGauge::criticalityRating, Comparator.reverseOrder())
                        .thenComparing(AssetGauge::code))
                .toList();

        return new DashboardResponse(
                riskRepository.count(),
                assetRepository.count(),
                threatRepository.count(),
                controlRepository.count(),
                distribution(riskRepository.countByCurrentLevel()),
                distribution(riskRepository.countByResidualLevel()),
                distribution(riskRepository.countByInherentLevel()),
                implemented,
                planned,
                totalLinks == 0 ? 0 : (int) Math.round(implemented * 100.0 / totalLinks),
                riskRepository.countOverdue(LocalDate.now(), STATUS_DONE),
                gauges,
                remediationTimeline(),
                namedCounts(riskRepository.countByTreatmentMethod()),
                namedCounts(riskRepository.countByMeasureStatus()));
    }

    /**
     * Cumulative "due" and "done" counts per month of deadline.
     *
     * <p>Two things are worth knowing here. First, a month with no deadlines
     * still gets a point: skipping it would let the x-axis jump from January to
     * June while the line kept its constant slope, drawing a gentle climb over
     * what was actually a cliff. Gaps have to be filled for a time axis to mean
     * anything.
     *
     * <p>Second, the counts are running totals, so both lines are monotonic.
     * The vertical gap between them at any month is the backlog at that date,
     * which is the single number this chart exists to show.
     */
    private List<TimelinePoint> remediationTimeline() {
        List<Object[]> rows = riskRepository.deadlinesWithStatus();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<YearMonth, long[]> byMonth = new HashMap<>();
        YearMonth first = null;
        YearMonth last = null;

        for (Object[] row : rows) {
            YearMonth month = YearMonth.from((LocalDate) row[0]);
            boolean done = STATUS_DONE.equals(row[1]);
            // index 0 = due that month, index 1 = of those, finished
            long[] counts = byMonth.computeIfAbsent(month, m -> new long[2]);
            counts[0]++;
            if (done) {
                counts[1]++;
            }
            if (first == null || month.isBefore(first)) {
                first = month;
            }
            if (last == null || month.isAfter(last)) {
                last = month;
            }
        }

        List<TimelinePoint> timeline = new ArrayList<>();
        long dueTotal = 0;
        long doneTotal = 0;
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            long[] counts = byMonth.getOrDefault(month, EMPTY_MONTH);
            dueTotal += counts[0];
            doneTotal += counts[1];
            timeline.add(new TimelinePoint(month.toString(), dueTotal, doneTotal));
        }
        return timeline;
    }

    /** Largest first - a chart legend reads better ranked than alphabetical. */
    private List<NamedCount> namedCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new NamedCount((String) row[0], ((Number) row[1]).longValue()))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed()
                        .thenComparing(NamedCount::label))
                .toList();
    }

    /** Expands a sparse GROUP BY result into all five levels, zeros included. */
    private List<LevelCount> distribution(List<Object[]> rows) {
        Map<Integer, Long> byLevel = new HashMap<>();
        for (Object[] row : rows) {
            byLevel.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        List<LevelCount> result = new ArrayList<>(5);
        // Descending: Критический first, matching how the register is read.
        for (int level = 5; level >= 1; level--) {
            RiskLevel rl = RiskLevel.ofLevel(level);
            result.add(new LevelCount(level, rl.getLabel(), byLevel.getOrDefault(level, 0L)));
        }
        return result;
    }

    private String labelOf(Integer level) {
        return level == null ? null : RiskLevel.ofLevel(level).getLabel();
    }
}

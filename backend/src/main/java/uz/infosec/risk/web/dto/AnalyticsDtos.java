package uz.infosec.risk.web.dto;

import java.util.List;

/** Payloads for the two read-only aggregate endpoints. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    // ------------------------------------------------------- risk matrix

    /**
     * One cell of the 5x5 grid.
     *
     * @param count     number of risks; null renders as an empty cell, matching
     *                  the workbook's IF(COUNTIFS(...)=0,"",...)
     * @param riskLevel what the a x t algorithm yields for this cell - it is a
     *                  property of the coordinates, so it is coloured even when
     *                  empty
     */
    public record MatrixCell(int assetRating, int threatRating, Integer count,
                             int riskLevel, String riskLabel) {
    }

    public record MatrixLegendItem(int value, String labelUz, String labelRu) {
    }

    public record RiskMatrixResponse(
            /** Rows: asset criticality 5..1, as the sheet is laid out. */
            List<Integer> assetRatings,
            /** Columns: threat level after implemented controls, 1..5. */
            List<Integer> threatRatings,
            List<MatrixCell> cells,
            int totalRisks,
            List<MatrixLegendItem> assetLegend,
            List<MatrixLegendItem> threatLegend,
            List<MatrixLegendItem> riskLegend) {
    }

    // ---------------------------------------------------------- dashboard

    /** One speedometer card: an asset and the worst risk currently on it. */
    public record AssetGauge(
            Long assetId,
            String code,
            String name,
            String criticality,
            int criticalityRating,
            int riskCount,
            /** Needle position: worst current risk level, null if no risks. */
            Integer worstCurrentLevel,
            String worstCurrentLabel,
            Integer worstResidualLevel,
            String worstResidualLabel) {
    }

    public record LevelCount(int level, String label, long count) {
    }

    /**
     * One month of the remediation timeline.
     *
     * <p>Counts are cumulative, not per-month. A per-month bar answers "how busy
     * was March", which nobody asks; the cumulative pair answers "is the plan
     * being kept", because the gap between the two lines IS the backlog.
     *
     * @param month     ISO yyyy-MM, so the client sorts and formats it itself
     * @param dueTotal  measures whose deadline has arrived by the end of this month
     * @param doneTotal how many of those are finished
     */
    public record TimelinePoint(String month, long dueTotal, long doneTotal) {
    }

    /** A label and its count - treatment methods, measure statuses. */
    public record NamedCount(String label, long count) {
    }

    public record DashboardResponse(
            long totalRisks,
            long totalAssets,
            long totalThreats,
            long totalControls,
            /** Distribution over the five levels, always all five entries. */
            List<LevelCount> currentDistribution,
            List<LevelCount> residualDistribution,
            /** Where risks would sit with no controls at all - the chart's baseline. */
            List<LevelCount> inherentDistribution,
            long implementedControlLinks,
            long plannedControlLinks,
            /** Percentage of risk-control links already implemented, 0..100. */
            int implementedPercent,
            /** Deadline passed and status is not "Выполнено". */
            long overdueMeasures,
            List<AssetGauge> assetGauges,
            /** Chronological, one entry per month that has at least one deadline. */
            List<TimelinePoint> remediationTimeline,
            List<NamedCount> treatmentBreakdown,
            List<NamedCount> statusBreakdown) {
    }
}

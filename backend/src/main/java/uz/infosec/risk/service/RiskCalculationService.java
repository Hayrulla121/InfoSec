package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import uz.infosec.risk.domain.RiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Every formula from the Excel workbook, in one place.
 *
 * <p>This class is deliberately <b>pure</b>: no repositories, no database, no
 * Spring dependencies beyond @Service. Given the same inputs it always returns
 * the same outputs, which is what makes the golden tests against real workbook
 * rows meaningful.
 *
 * <p>Excel source columns are cited per method so any future disagreement with
 * the workbook can be traced to an exact cell.
 */
@Service
public class RiskCalculationService {

    /** Threat scores are stored with 4 decimals (DECIMAL(8,4)). */
    public static final int SCORE_SCALE = 4;

    /** Intermediate scale, kept higher so rounding happens once at the end. */
    private static final int WORKING_SCALE = 10;

    private static final String[] THREAT_LABELS = {
            "Незначительный", "Низкий", "Средний", "Высокий", "Очень высокий"
    };

    /**
     * Excel helper columns L-P: {@code =IF(C2="",0,IF(C2<6,C2,5))}.
     * Blank becomes 0, anything above 5 is capped at 5.
     */
    public int clampCriterion(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return Math.min(value, 5);
    }

    /** Excel column Q: {@code =SUM(L2:P2)} -> 0..25. */
    public int totalScore(Integer discoverability, Integer repeatability, Integer exploitability,
                          Integer affectedUsers, Integer damage) {
        return clampCriterion(discoverability)
                + clampCriterion(repeatability)
                + clampCriterion(exploitability)
                + clampCriterion(affectedUsers)
                + clampCriterion(damage);
    }

    /**
     * Excel column R / BH / BV:
     * {@code =IF(s<6,1,IF(s<11,2,IF(s<16,3,IF(s<21,4,5))))}
     *
     * <p>Applied at every stage - to the raw score AND to reduced scores - which
     * is why it takes a BigDecimal rather than an int.
     */
    public int ratingFromScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(6)) < 0) return 1;
        if (score.compareTo(BigDecimal.valueOf(11)) < 0) return 2;
        if (score.compareTo(BigDecimal.valueOf(16)) < 0) return 3;
        if (score.compareTo(BigDecimal.valueOf(21)) < 0) return 4;
        return 5;
    }

    public int ratingFromScore(int score) {
        return ratingFromScore(BigDecimal.valueOf(score));
    }

    /** Excel column H: rating 1-5 -> its Russian label. */
    public String threatLevelLabel(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Threat rating must be 1..5, got " + rating);
        }
        return THREAT_LABELS[rating - 1];
    }

    /**
     * Excel columns AQ-AW (implemented) and BC-BG (planned), each of the form
     * {@code =prev - prev*pct}, i.e. {@code score *= (1 - pct)}.
     *
     * <p>The chain is mathematically order-independent (multiplication is
     * commutative), which is why the apply_order column is display-only.
     *
     * <p>Excel is limited to 7 implemented + 5 planned columns; this loop has no
     * such limit.
     */
    public BigDecimal applyReductions(BigDecimal baseScore, List<BigDecimal> reductionPercentages) {
        BigDecimal score = baseScore.setScale(WORKING_SCALE, RoundingMode.HALF_UP);
        for (BigDecimal pct : reductionPercentages) {
            if (pct == null) {
                continue;
            }
            // score - score*pct, written exactly as the spreadsheet does it.
            score = score.subtract(score.multiply(pct));
        }
        return score.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Excel columns AI / BU / BW - the qualitative risk algorithm, transcribed
     * from the sheet's own pseudocode (Матрица рисков!T2):
     *
     * <pre>
     * if ( a*t >= 20 )                              r = 5
     * else if (( a=1 & t<3) | ( t=1 & a<4 ))        r = 1
     * else if ( t>2 & a*t>=10 )                     r = 4
     * else if (( t<4 & t*a>3 & t*a<6) | ( t=3 & a<3 )) r = 2
     * else                                          r = 3
     * </pre>
     *
     * <p>Order matters: the branches are not mutually exclusive, so rearranging
     * them silently changes results. It is written top-down exactly as Excel
     * nests its IFs.
     *
     * @param assetRating  a - asset criticality 1..5
     * @param threatRating t - threat level 1..5 at the stage being evaluated
     */
    public RiskLevel classify(int assetRating, int threatRating) {
        requireRange(assetRating, "assetRating");
        requireRange(threatRating, "threatRating");

        int a = assetRating;
        int t = threatRating;
        int p = a * t;

        if (p >= 20) {
            return RiskLevel.CRITICAL;
        }
        if ((a == 1 && t < 3) || (t == 1 && a < 4)) {
            return RiskLevel.NEGLIGIBLE;
        }
        if (t > 2 && p >= 10) {
            return RiskLevel.HIGH;
        }
        if ((t < 4 && p > 3 && p < 6) || (t == 3 && a < 3)) {
            return RiskLevel.LOW;
        }
        return RiskLevel.MEDIUM;
    }

    private void requireRange(int value, String name) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException(name + " must be 1..5, got " + value);
        }
    }
}

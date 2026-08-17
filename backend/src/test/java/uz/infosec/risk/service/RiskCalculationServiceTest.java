package uz.infosec.risk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uz.infosec.risk.domain.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Golden tests for the Excel formula engine.
 *
 * <p>No Spring context: the service is pure, so these run in milliseconds and
 * pin the arithmetic independently of the database or the web layer. If a
 * business rule ever changes, exactly one of these should go red and tell you
 * which cell of the workbook you have diverged from.
 */
class RiskCalculationServiceTest {

    private final RiskCalculationService calc = new RiskCalculationService();

    @Nested
    @DisplayName("DREAD scoring (Excel cols L-R)")
    class DreadScoring {

        @Test
        void clampsLikeTheHelperColumns() {
            // =IF(C2="",0,IF(C2<6,C2,5))
            assertThat(calc.clampCriterion(null)).isZero();
            assertThat(calc.clampCriterion(0)).isZero();
            assertThat(calc.clampCriterion(3)).isEqualTo(3);
            assertThat(calc.clampCriterion(5)).isEqualTo(5);
            assertThat(calc.clampCriterion(9)).isEqualTo(5);
            assertThat(calc.clampCriterion(-2)).isZero();
        }

        /** Threat У1 of the workbook: 2,2,4,3,2 -> 13. */
        @Test
        void sumsTheFiveCriteria() {
            assertThat(calc.totalScore(2, 2, 4, 3, 2)).isEqualTo(13);
        }

        /** Threat У2: 3,1,1,2,2 -> 9. Threat У3: 4,5,5,2,2 -> 18. */
        @Test
        void matchesOtherWorkbookRows() {
            assertThat(calc.totalScore(3, 1, 1, 2, 2)).isEqualTo(9);
            assertThat(calc.totalScore(4, 5, 5, 2, 2)).isEqualTo(18);
        }

        @Test
        void scoreIsBoundedByZeroAndTwentyFive() {
            assertThat(calc.totalScore(0, 0, 0, 0, 0)).isZero();
            assertThat(calc.totalScore(5, 5, 5, 5, 5)).isEqualTo(25);
            assertThat(calc.totalScore(99, 99, 99, 99, 99)).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Rating thresholds (Excel col R)")
    class RatingThresholds {

        /** Every boundary of =IF(s<6,1,IF(s<11,2,IF(s<16,3,IF(s<21,4,5)))). */
        @ParameterizedTest(name = "score {0} -> rating {1}")
        @CsvSource({
                "0, 1", "5, 1",
                "6, 2", "10, 2",
                "11, 3", "15, 3",
                "16, 4", "20, 4",
                "21, 5", "25, 5",
        })
        void mapsScoreToRating(int score, int expected) {
            assertThat(calc.ratingFromScore(score)).isEqualTo(expected);
        }

        /** Reduced scores are fractional, and the same thresholds apply. */
        @ParameterizedTest(name = "score {0} -> rating {1}")
        @CsvSource({
                "5.9999, 1", "6.0, 2",
                "10.4, 2", "10.9999, 2", "11.0, 3",
                "4.16, 1", "8.32, 2",
        })
        void mapsFractionalScoreToRating(String score, int expected) {
            assertThat(calc.ratingFromScore(new BigDecimal(score))).isEqualTo(expected);
        }

        @Test
        void labelsMatchTheWorkbook() {
            assertThat(calc.threatLevelLabel(1)).isEqualTo("Незначительный");
            assertThat(calc.threatLevelLabel(2)).isEqualTo("Низкий");
            assertThat(calc.threatLevelLabel(3)).isEqualTo("Средний");
            assertThat(calc.threatLevelLabel(4)).isEqualTo("Высокий");
            assertThat(calc.threatLevelLabel(5)).isEqualTo("Очень высокий");
        }
    }

    @Nested
    @DisplayName("Control reduction chain (Excel cols AQ-AW, BC-BG)")
    class ReductionChain {

        /**
         * THE golden case from the spec, traced through the workbook:
         * base 13, implemented -20% -> 10.4, then planned -20% and -50% -> 4.16.
         */
        @Test
        void reproducesTheSpecWorkedExample() {
            BigDecimal base = BigDecimal.valueOf(13);

            BigDecimal current = calc.applyReductions(base, List.of(new BigDecimal("0.20")));
            assertThat(current).isEqualByComparingTo("10.4");
            assertThat(calc.ratingFromScore(current)).isEqualTo(2);

            BigDecimal residual = calc.applyReductions(current,
                    List.of(new BigDecimal("0.20"), new BigDecimal("0.50")));
            assertThat(residual).isEqualByComparingTo("4.16");
            assertThat(calc.ratingFromScore(residual)).isEqualTo(1);
        }

        @Test
        void noControlsLeavesTheScoreUnchanged() {
            assertThat(calc.applyReductions(BigDecimal.valueOf(13), List.of()))
                    .isEqualByComparingTo("13");
        }

        /** Multiplication commutes, so apply_order is presentation only. */
        @Test
        void chainOrderDoesNotChangeTheResult() {
            BigDecimal base = BigDecimal.valueOf(18);
            BigDecimal forwards = calc.applyReductions(base,
                    List.of(new BigDecimal("0.30"), new BigDecimal("0.15"), new BigDecimal("0.50")));
            BigDecimal backwards = calc.applyReductions(base,
                    List.of(new BigDecimal("0.50"), new BigDecimal("0.15"), new BigDecimal("0.30")));
            assertThat(forwards).isEqualByComparingTo(backwards);
        }

        @Test
        void aFullyEffectiveControlZeroesTheScore() {
            assertThat(calc.applyReductions(BigDecimal.valueOf(25), List.of(BigDecimal.ONE)))
                    .isEqualByComparingTo("0");
        }

        /**
         * Reductions compound, they do not add up: 50% then 50% leaves 25%,
         * not 0%. Getting this wrong is the classic way to under-report risk.
         */
        @Test
        void reductionsCompoundRatherThanSum() {
            BigDecimal result = calc.applyReductions(BigDecimal.valueOf(20),
                    List.of(new BigDecimal("0.50"), new BigDecimal("0.50")));
            assertThat(result).isEqualByComparingTo("5");
        }

        /**
         * Chained percentages are exactly why the column is DECIMAL and the
         * maths is BigDecimal: with doubles, 13 * 0.8 * 0.8 * 0.5 drifts off
         * 4.16 and could tip across a rating threshold.
         */
        @Test
        void arithmeticIsExactNotFloatingPoint() {
            BigDecimal result = calc.applyReductions(BigDecimal.valueOf(13),
                    List.of(new BigDecimal("0.20"), new BigDecimal("0.20"), new BigDecimal("0.50")));
            assertThat(result.stripTrailingZeros().toPlainString()).isEqualTo("4.16");
        }
    }

    @Nested
    @DisplayName("Risk classification a x t (Excel cols AI/BU/BW)")
    class Classification {

        /** The four cases named explicitly in the specification. */
        @Test
        void specNamedCases() {
            // p=10 but t is not > 2, so the HIGH branch does not fire.
            assertThat(calc.classify(5, 2)).isEqualTo(RiskLevel.MEDIUM);
            assertThat(calc.classify(5, 4)).isEqualTo(RiskLevel.CRITICAL);
            assertThat(calc.classify(1, 2)).isEqualTo(RiskLevel.NEGLIGIBLE);
            assertThat(calc.classify(2, 2)).isEqualTo(RiskLevel.LOW);
            assertThat(calc.classify(3, 3)).isEqualTo(RiskLevel.MEDIUM);
        }

        @Test
        void criticalWheneverProductReachesTwenty() {
            assertThat(calc.classify(4, 5)).isEqualTo(RiskLevel.CRITICAL);
            assertThat(calc.classify(5, 4)).isEqualTo(RiskLevel.CRITICAL);
            assertThat(calc.classify(5, 5)).isEqualTo(RiskLevel.CRITICAL);
        }

        @Test
        void negligibleOnlyForTinyAssetsOrTinyThreats() {
            assertThat(calc.classify(1, 1)).isEqualTo(RiskLevel.NEGLIGIBLE);
            assertThat(calc.classify(1, 2)).isEqualTo(RiskLevel.NEGLIGIBLE);
            assertThat(calc.classify(2, 1)).isEqualTo(RiskLevel.NEGLIGIBLE);
            assertThat(calc.classify(3, 1)).isEqualTo(RiskLevel.NEGLIGIBLE);

            // Principle 2 from the Матрица рисков legend: a highly critical
            // asset can never carry a negligible risk. At a=4 and a=5 the
            // "t==1 && a<4" guard stops firing, so the score falls through to
            // the LOW branch (t<4, 3 < a*t < 6) instead.
            assertThat(calc.classify(4, 1)).isEqualTo(RiskLevel.LOW);
            assertThat(calc.classify(5, 1)).isEqualTo(RiskLevel.LOW);
        }

        /**
         * Full 5x5 grid, transcribed from applying the workbook algorithm.
         * This is the single most valuable test in the project: the risk matrix,
         * the dashboard and every report are downstream of exactly these 25 cells.
         */
        @ParameterizedTest(name = "a={0} t={1} -> {2}")
        @CsvSource({
                "1,1,NEGLIGIBLE", "1,2,NEGLIGIBLE", "1,3,LOW",        "1,4,MEDIUM",   "1,5,MEDIUM",
                "2,1,NEGLIGIBLE", "2,2,LOW",        "2,3,LOW",        "2,4,MEDIUM",   "2,5,HIGH",
                "3,1,NEGLIGIBLE", "3,2,MEDIUM",     "3,3,MEDIUM",     "3,4,HIGH",     "3,5,HIGH",
                "4,1,LOW",        "4,2,MEDIUM",     "4,3,HIGH",       "4,4,HIGH",     "4,5,CRITICAL",
                "5,1,LOW",        "5,2,MEDIUM",     "5,3,HIGH",       "5,4,CRITICAL", "5,5,CRITICAL",
        })
        void fullMatrix(int a, int t, RiskLevel expected) {
            assertThat(calc.classify(a, t)).isEqualTo(expected);
        }

        @Test
        void rejectsOutOfRangeInputs() {
            assertThatThrownBy(() -> calc.classify(0, 3))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> calc.classify(3, 6))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

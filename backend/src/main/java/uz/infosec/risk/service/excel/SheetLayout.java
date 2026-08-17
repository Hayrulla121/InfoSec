package uz.infosec.risk.service.excel;

/**
 * The exported workbook's column layout, transcribed from the source file.
 *
 * <p>Kept as named constants rather than inline strings so a formula that
 * points at the wrong column is a compile error rather than a wrong number in
 * a bank's risk report.
 */
public final class SheetLayout {

    private SheetLayout() {
    }

    // Sheet names must match the original exactly - cross-sheet formulas
    // reference them by name.
    public static final String S_THREAT_MODEL = "Ma'lumot - Tahdidlar modeli";
    public static final String S_ASSETS = "Реестр ключевых ИА";
    public static final String S_THREATS = "Реестр угроз";
    public static final String S_MATRIX = "Матрица рисков";
    public static final String S_RISKS = "Реестр рисков";
    public static final String S_CONTROLS = "Риск-контроль";
    public static final String S_TECH = "Техническая страница";
    public static final String S_INFO_SYSTEMS = "Перечень инфосистем Банка";

    /** Excel's hard ceiling on the Реестр рисков sheet: columns R..X. */
    public static final String[] IMPLEMENTED_ID_COLS = {"R", "S", "T", "U", "V", "W", "X"};
    /** Columns Z..AD. */
    public static final String[] PLANNED_ID_COLS = {"Z", "AA", "AB", "AC", "AD"};

    /** "Снижение 1..7" - reduction % fetched per implemented control. */
    public static final String[] REDUCTION_COLS = {"AJ", "AK", "AL", "AM", "AN", "AO", "AP"};
    /** "Счет угрозы после снижения 1..7" - the implemented chain. */
    public static final String[] AFTER_IMPL_COLS = {"AQ", "AR", "AS", "AT", "AU", "AV", "AW"};
    /** "План 1..5" - reduction % fetched per planned control. */
    public static final String[] PLAN_REDUCTION_COLS = {"AX", "AY", "AZ", "BA", "BB"};
    /** "Счет угрозы после обработки 1..5" - the planned chain. */
    public static final String[] AFTER_PLAN_COLS = {"BC", "BD", "BE", "BF", "BG"};
    /** "Название 1..7" - implemented control names, joined by TEXTJOIN into H. */
    public static final String[] IMPL_NAME_COLS = {"BI", "BJ", "BK", "BL", "BM", "BN", "BO"};
    /** "Название план 1..5" - planned control names, joined into L. */
    public static final String[] PLAN_NAME_COLS = {"BP", "BQ", "BR", "BS", "BT"};

    public static final int MAX_IMPLEMENTED = IMPLEMENTED_ID_COLS.length; // 7
    public static final int MAX_PLANNED = PLANNED_ID_COLS.length;         // 5

    // Single-column landmarks on Реестр рисков.
    public static final String C_ASSET_NAME = "C";
    public static final String C_THREAT_DESC = "E";
    public static final String C_INDICATORS = "G";
    public static final String C_IMPL_JOIN = "H";
    public static final String C_RISK_LEVEL = "I";
    public static final String C_PLAN_JOIN = "L";
    public static final String C_RESIDUAL = "M";
    public static final String C_ASSET_RATING = "AF";
    public static final String C_THREAT_RATING = "AG";
    public static final String C_THREAT_SCORE = "AH";
    public static final String C_RISK = "AI";
    public static final String C_RATING_AFTER_PLAN = "BH";
    public static final String C_RESIDUAL_RISK = "BU";
    public static final String C_RATING_AFTER_CTRL = "BV";
    public static final String C_RISK_AFTER_CTRL = "BW";

    /**
     * The a x t classification, as an Excel formula.
     *
     * <p>Byte-for-byte the same nested IF the source workbook uses, so anyone
     * comparing the two files sees identical logic.
     *
     * @param a cell reference holding the asset rating, e.g. "AF2"
     * @param t cell reference holding the threat rating, e.g. "AG2"
     */
    public static String classifyFormula(String a, String t) {
        return "IF(OR(%s=\"\",%s=\"\"),\"\","
                .formatted(t, a)
                + "IF(%s*%s>=20,\"Критический\",".formatted(a, t)
                + "IF(OR(AND(%s=1,%s<3),AND(%s=1,%s<4)),\"Незначительный\",".formatted(a, t, t, a)
                + "IF(AND(%s>2,%s*%s>=10),\"Высокий\",".formatted(t, a, t)
                + "IF(OR(AND(%s<4,%s*%s>3,%s*%s<6),AND(%s=3,%s<3)),\"Низкий\","
                .formatted(t, t, a, t, a, t, a)
                + "\"Средний\")))))";
    }

    /**
     * Score -> rating 1..5, the threshold ladder from Excel column R.
     *
     * @param score cell holding the (possibly reduced) threat score
     * @param guard cell that must be non-empty for the row to count
     */
    public static String ratingFormula(String score, String guard) {
        return "IF(%s=\"\",\"\",IF(%s<6,1,IF(%s<11,2,IF(%s<16,3,IF(%s<21,4,5)))))"
                .formatted(guard, score, score, score, score);
    }
}

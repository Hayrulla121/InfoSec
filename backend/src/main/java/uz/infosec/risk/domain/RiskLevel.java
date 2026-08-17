package uz.infosec.risk.domain;

/**
 * Risk level 1-5 with the Russian label the workbook uses.
 * Shared by inherent, current and residual results.
 */
public enum RiskLevel {

    NEGLIGIBLE(1, "Незначительный"),
    LOW(2, "Низкий"),
    MEDIUM(3, "Средний"),
    HIGH(4, "Высокий"),
    CRITICAL(5, "Критический");

    private final int level;
    private final String label;

    RiskLevel(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public static RiskLevel ofLevel(int level) {
        for (RiskLevel value : values()) {
            if (value.level == level) {
                return value;
            }
        }
        throw new IllegalArgumentException("No risk level " + level);
    }
}

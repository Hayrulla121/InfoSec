package uz.infosec.risk.domain;

/**
 * The four dictionaries on the Техническая страница sheet.
 *
 * <p>{@code numericRequired} marks the two whose numeric_value participates in
 * the risk calculation: an asset's criticality becomes {@code a} and a threat's
 * level becomes {@code t} in the a x t classification. Editing those labels
 * changes computed risk levels, which is why the UI warns about it.
 */
public enum DictType {

    ASSET_CRITICALITY("Значимость актива", "dict.assetCriticality", true),
    THREAT_LEVEL("Уровень угрозы", "dict.threatLevel", true),
    TREATMENT_METHOD("Метод управления риском", "dict.treatmentMethod", false),
    MEASURE_STATUS("Статус мероприятий", "dict.measureStatus", false);

    private final String title;
    private final String titleCode;
    private final boolean numericRequired;

    DictType(String title, String titleCode, boolean numericRequired) {
        this.title = title;
        this.titleCode = titleCode;
        this.numericRequired = numericRequired;
    }

    /**
     * Russian title, kept because the Excel export writes these as sheet
     * headings and must match the source workbook regardless of UI language.
     */
    public String getTitle() {
        return title;
    }

    /** Message key, so error text can be shown in the user's language. */
    public String getTitleCode() {
        return titleCode;
    }

    public boolean isNumericRequired() {
        return numericRequired;
    }
}

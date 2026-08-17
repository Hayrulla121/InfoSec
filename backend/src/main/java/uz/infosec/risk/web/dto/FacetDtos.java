package uz.infosec.risk.web.dto;

/** Payloads for the column-filter dropdowns. */
public final class FacetDtos {

    private FacetDtos() {
    }

    /**
     * One selectable option of a column filter.
     *
     * @param value the stored value, sent back verbatim as the filter parameter
     * @param count how many rows in the whole registry hold it
     */
    public record FacetValue(String value, long count) {
    }
}

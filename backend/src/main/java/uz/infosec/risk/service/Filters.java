package uz.infosec.risk.service;

/**
 * Query-parameter normalisation shared by every registry search.
 *
 * <p>Spring binds a missing parameter to {@code null} but an empty one
 * ({@code ?criticality=}) to the empty string. The two must mean the same
 * thing - "no filter" - because that is exactly what a browser sends when the
 * user clears a dropdown. Without this, clearing a filter would search for rows
 * whose column literally equals "" and the table would go blank.
 */
public final class Filters {

    private Filters() {
    }

    /** Blank (null, empty or whitespace) becomes null; anything else is trimmed. */
    public static String orNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

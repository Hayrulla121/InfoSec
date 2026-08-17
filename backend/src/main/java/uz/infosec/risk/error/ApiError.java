package uz.infosec.risk.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The one error shape every failing endpoint returns, so the frontend has a
 * single code path for displaying problems.
 *
 * <p>{@code @JsonInclude(NON_EMPTY)} keeps fieldErrors out of the JSON when
 * there are none, rather than emitting {@code "fieldErrors": []}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp,
                       int status,
                       String message,
                       List<FieldError> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(Instant.now(), status, message, List.of());
    }

    public static ApiError of(int status, String message, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, message, fieldErrors);
    }

    /** One failed validation constraint: which field, and what was wrong. */
    public record FieldError(String field, String message) {
    }
}

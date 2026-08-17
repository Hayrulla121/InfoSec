package uz.infosec.risk.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request and response shapes for the three registries.
 *
 * <p>Note the asymmetry: request records carry only the fields a user may set,
 * response records also carry the computed ones. That is what makes it
 * impossible for a client to POST its own {@code rating} and have the server
 * believe it.
 */
public final class RegistryDtos {

    private RegistryDtos() {
    }

    // ---------------------------------------------------------------- assets

    public record AssetRequest(
            @Size(max = 255) String name,
            @Size(max = 255) String scope,
            @Size(max = 255) String infoCategory,
            @NotBlank @Size(max = 32) String criticality,
            @Size(max = 8) String securityClass,
            Long infoSystemId) {
    }

    public record AssetResponse(
            Long id,
            String code,
            String name,
            String scope,
            String infoCategory,
            String criticality,
            /** computed from criticality via the dictionary */
            int criticalityRating,
            String securityClass,
            Long infoSystemId,
            String infoSystemName,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {
    }

    // --------------------------------------------------------------- threats

    /**
     * The five DREAD criteria. @Min/@Max reject out-of-range values with a 400
     * and a per-field message; RiskCalculationService additionally clamps, so
     * the stored score is sane even if validation is ever bypassed.
     */
    public record ThreatRequest(
            @NotBlank String description,
            @NotNull @Min(0) @Max(5) Integer discoverability,
            @NotNull @Min(0) @Max(5) Integer repeatability,
            @NotNull @Min(0) @Max(5) Integer exploitability,
            @NotNull @Min(0) @Max(5) Integer affectedUsers,
            @NotNull @Min(0) @Max(5) Integer damage) {
    }

    public record ThreatResponse(
            Long id,
            String code,
            String description,
            int discoverability,
            int repeatability,
            int exploitability,
            int affectedUsers,
            int damage,
            /** computed: sum of the five, 0..25 */
            int totalScore,
            /** computed: 1..5 */
            int rating,
            /** computed: Незначительный .. Очень высокий */
            String levelLabel,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {
    }

    // -------------------------------------------------------------- controls

    public record ControlRequest(
            @NotBlank @Size(max = 500) String name,
            String description,
            @NotBlank @Size(max = 32) String treatmentMethod,
            @NotNull
            @DecimalMin(value = "0.0", message = "must be between 0 and 1")
            @DecimalMax(value = "1.0", message = "must be between 0 and 1")
            @Digits(integer = 1, fraction = 2, message = "at most two decimal places")
            BigDecimal reductionPct,
            boolean implemented) {
    }

    public record ControlResponse(
            Long id,
            String code,
            String name,
            String description,
            String treatmentMethod,
            BigDecimal reductionPct,
            boolean implemented,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {
    }

    // ---------------------------------------------------------- info systems

    public record InfoSystemRequest(
            @NotBlank @Size(max = 255) String name,
            String description,
            String hosting,
            String usagePurpose,
            @Size(max = 32) String dataFormat,
            @Size(max = 32) String confidentiality,
            @Size(max = 8) String integrity,
            @Size(max = 8) String availability,
            @Size(max = 64) String updateFrequency,
            String usersInfo,
            @Size(max = 255) String owner) {
    }

    public record InfoSystemResponse(
            Long id,
            String code,
            String name,
            String description,
            String hosting,
            String usagePurpose,
            String dataFormat,
            String confidentiality,
            String integrity,
            String availability,
            String updateFrequency,
            String usersInfo,
            String owner,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {
    }
}

package uz.infosec.risk.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.infosec.risk.domain.ControlType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RiskDtos {

    private RiskDtos() {
    }

    public record RiskRequest(
            @NotNull Long assetId,
            @NotNull Long threatId,
            @NotNull @Size(max = 500) String name,
            String indicators,
            @Size(max = 255) String owner,
            @Size(max = 32) String treatmentMethod,
            @Size(max = 64) String measureStatus,
            LocalDate implementationDeadline,
            String comment) {
    }

    /**
     * One attached control, as shown in the risk detail drawer.
     *
     * <p>{@code scoreBefore} / {@code scoreAfter} are this link's own step of
     * the reduction chain, supplied by the server so the UI can show the
     * arithmetic ("10.4000 − 10.4000 × 0.20 = 8.3200") without re-running any
     * formula of its own. The implemented chain starts from the raw threat
     * score; the planned chain continues from the current score, exactly as
     * Excel's BC column starts from AW.
     */
    public record RiskControlDto(
            Long linkId,
            Long controlId,
            String controlCode,
            String controlName,
            String treatmentMethod,
            BigDecimal reductionPct,
            ControlType controlType,
            int applyOrder,
            BigDecimal scoreBefore,
            BigDecimal scoreAfter) {
    }

    public record AttachControlRequest(
            @NotNull Long controlId,
            @NotNull ControlType type) {
    }

    /**
     * The three computed stages, grouped so the UI can render
     * inherent -> current -> residual as one progression.
     */
    public record RiskStage(
            BigDecimal score,
            Integer threatRating,
            /** The rating in words - "Средний" for 3. Excel column H. */
            String threatLabel,
            Integer riskLevel,
            String riskLabel) {
    }

    public record RiskResponse(
            Long id,
            String code,
            Long assetId,
            String assetCode,
            String assetName,
            String assetCriticality,
            int assetRating,
            Long threatId,
            String threatCode,
            String threatDescription,
            int threatTotalScore,
            String name,
            String indicators,
            String owner,
            String treatmentMethod,
            String measureStatus,
            LocalDate implementationDeadline,
            String comment,
            RiskStage inherent,
            RiskStage current,
            RiskStage residual,
            /** Replaces Excel's TEXTJOIN columns H and L. */
            List<RiskControlDto> implementedControls,
            List<RiskControlDto> plannedControls,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {
    }
}

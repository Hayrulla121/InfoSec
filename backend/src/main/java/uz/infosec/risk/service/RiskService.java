package uz.infosec.risk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.error.ConflictException;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.*;
import uz.infosec.risk.web.dto.RiskDtos.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Реестр рисков. */
@Service
public class RiskService {

    /**
     * Only the pure label lookup is needed here, and toDto is static, so a
     * single shared instance beats threading the bean through every call site.
     * RiskCalculationService holds no state - that is what makes this safe.
     */
    private static final RiskCalculationService THREAT_LABELS = new RiskCalculationService();

    private final RiskRepository riskRepository;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final ControlRepository controlRepository;
    private final RiskRecalculationService recalculation;
    private final CodeGenerator codeGenerator;

    public RiskService(RiskRepository riskRepository,
                       AssetRepository assetRepository,
                       ThreatRepository threatRepository,
                       ControlRepository controlRepository,
                       RiskRecalculationService recalculation,
                       CodeGenerator codeGenerator) {
        this.riskRepository = riskRepository;
        this.assetRepository = assetRepository;
        this.threatRepository = threatRepository;
        this.controlRepository = controlRepository;
        this.recalculation = recalculation;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<RiskResponse> search(String query, Integer assetRating, Integer threatRating,
                                     String treatmentMethod, String measureStatus,
                                     String currentRiskLabel, String residualRiskLabel,
                                     Pageable pageable) {
        return riskRepository.search(
                        query, assetRating, threatRating,
                        Filters.orNull(treatmentMethod),
                        Filters.orNull(measureStatus),
                        Filters.orNull(currentRiskLabel),
                        Filters.orNull(residualRiskLabel),
                        pageable)
                .map(RiskService::toDto);
    }

    @Transactional(readOnly = true)
    public RiskResponse findById(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public RiskResponse create(RiskRequest request) {
        if (riskRepository.existsByAssetIdAndThreatId(request.assetId(), request.threatId())) {
            throw ConflictException.of("risk.duplicatePair");
        }

        Risk risk = new Risk();
        risk.setCode(codeGenerator.next(Risk.CODE_PREFIX, riskRepository.findAllCodes()));
        risk.setAsset(assetRepository.findById(request.assetId())
                .orElseThrow(() -> NotFoundException.of("entity.asset", request.assetId())));
        risk.setThreat(threatRepository.findById(request.threatId())
                .orElseThrow(() -> NotFoundException.of("entity.threat", request.threatId())));
        applyFields(risk, request);

        // A brand-new risk has no controls yet, so current == residual == inherent.
        recalculation.recalculate(risk);
        return toDto(riskRepository.save(risk));
    }

    @Transactional
    public RiskResponse update(Long id, RiskRequest request) {
        Risk risk = load(id);

        // Changing either side of the pair changes a and t, so re-check the
        // uniqueness rule before touching anything.
        boolean pairChanged = !risk.getAsset().getId().equals(request.assetId())
                || !risk.getThreat().getId().equals(request.threatId());
        if (pairChanged) {
            if (riskRepository.existsByAssetIdAndThreatId(request.assetId(), request.threatId())) {
                throw ConflictException.of("risk.duplicatePair");
            }
            risk.setAsset(assetRepository.findById(request.assetId())
                    .orElseThrow(() -> NotFoundException.of("entity.asset", request.assetId())));
            risk.setThreat(threatRepository.findById(request.threatId())
                    .orElseThrow(() -> NotFoundException.of("entity.threat", request.threatId())));
        }

        applyFields(risk, request);
        recalculation.recalculate(risk);
        return toDto(risk);
    }

    @Transactional
    public void delete(Long id) {
        // risk_controls rows go with it (ON DELETE CASCADE + orphanRemoval).
        riskRepository.delete(load(id));
    }

    // ------------------------------------------------------- controls

    @Transactional(readOnly = true)
    public List<RiskControlDto> listControls(Long riskId) {
        Risk risk = load(riskId);
        // Both chains, each threaded from its own starting score - implemented
        // first, because the planned one continues where it leaves off.
        List<RiskControlDto> all = new ArrayList<>(controlsOfType(
                risk, ControlType.IMPLEMENTED,
                BigDecimal.valueOf(risk.getThreat().getTotalScore())));
        all.addAll(controlsOfType(risk, ControlType.PLANNED, risk.getCurrentScore()));
        return all;
    }

    @Transactional
    public RiskResponse attachControl(Long riskId, AttachControlRequest request) {
        Risk risk = load(riskId);

        boolean already = risk.getControls().stream()
                .anyMatch(rc -> rc.getControl().getId().equals(request.controlId()));
        if (already) {
            throw ConflictException.of("risk.controlAlreadyAttached");
        }

        Control control = controlRepository.findById(request.controlId())
                .orElseThrow(() -> NotFoundException.of("entity.control", request.controlId()));

        RiskControl link = new RiskControl();
        link.setRisk(risk);
        link.setControl(control);
        link.setControlType(request.type());
        link.setApplyOrder(risk.getControls().size() + 1);
        risk.getControls().add(link);

        // The reduction chain just changed: refresh current and residual.
        recalculation.recalculate(risk);
        return toDto(risk);
    }

    @Transactional
    public RiskResponse detachControl(Long riskId, Long linkId) {
        Risk risk = load(riskId);
        boolean removed = risk.getControls().removeIf(rc -> rc.getId().equals(linkId));
        if (!removed) {
            throw NotFoundException.of("entity.riskControlLink", linkId);
        }
        recalculation.recalculate(risk);
        return toDto(risk);
    }

    private void applyFields(Risk risk, RiskRequest request) {
        risk.setName(request.name());
        risk.setIndicators(request.indicators());
        risk.setOwner(request.owner());
        risk.setTreatmentMethod(request.treatmentMethod());
        risk.setMeasureStatus(request.measureStatus());
        risk.setImplementationDeadline(request.implementationDeadline());
        risk.setComment(request.comment());
    }

    private Risk load(Long id) {
        return riskRepository.findByIdWithRefs(id)
                .orElseThrow(() -> NotFoundException.of("entity.risk", id));
    }

    // ---------------------------------------------------------- mapping

    public static RiskResponse toDto(Risk r) {
        Asset a = r.getAsset();
        Threat t = r.getThreat();

        return new RiskResponse(
                r.getId(), r.getCode(),
                a.getId(), a.getCode(), a.getName(), a.getCriticality(), a.getCriticalityRating(),
                t.getId(), t.getCode(), t.getDescription(), t.getTotalScore(),
                r.getName(), r.getIndicators(), r.getOwner(),
                r.getTreatmentMethod(), r.getMeasureStatus(),
                r.getImplementationDeadline(), r.getComment(),
                new RiskStage(null, r.getInherentThreatRating(),
                        threatWord(r.getInherentThreatRating()),
                        r.getInherentRiskLevel(), r.getInherentRiskLabel()),
                new RiskStage(r.getCurrentScore(), r.getCurrentThreatRating(),
                        threatWord(r.getCurrentThreatRating()),
                        r.getCurrentRiskLevel(), r.getCurrentRiskLabel()),
                new RiskStage(r.getResidualScore(), r.getResidualThreatRating(),
                        threatWord(r.getResidualThreatRating()),
                        r.getResidualRiskLevel(), r.getResidualRiskLabel()),
                // The implemented chain starts at the raw threat score, the
                // planned chain continues from where the implemented one ended.
                controlsOfType(r, ControlType.IMPLEMENTED, BigDecimal.valueOf(t.getTotalScore())),
                controlsOfType(r, ControlType.PLANNED, r.getCurrentScore()),
                r.getCreatedAt(), r.getCreatedBy(), r.getUpdatedAt(), r.getUpdatedBy());
    }

    /** Excel column H: a threat rating in words. Null stays null. */
    private static String threatWord(Integer rating) {
        return rating == null ? null : THREAT_LABELS.threatLevelLabel(rating);
    }

    /**
     * Maps one chain of links, threading the running score through them so each
     * DTO carries the step it is responsible for.
     *
     * <p>Order is by apply_order: the arithmetic is commutative, so the result
     * does not depend on it, but a chain shown out of order would not read as a
     * chain. A null base (a risk not yet calculated) leaves the steps null
     * rather than inventing a starting point.
     */
    private static List<RiskControlDto> controlsOfType(Risk risk, ControlType type, BigDecimal base) {
        BigDecimal running = base == null ? null : RiskCalculationService.toWorkingScale(base);
        List<RiskControlDto> out = new ArrayList<>();
        List<RiskControl> links = risk.getControls().stream()
                .filter(rc -> rc.getControlType() == type)
                .sorted(Comparator.comparingInt(RiskControl::getApplyOrder))
                .toList();

        for (RiskControl rc : links) {
            Control c = rc.getControl();
            BigDecimal before = running;
            BigDecimal after = running == null
                    ? null : RiskCalculationService.reduceOnce(running, c.getReductionPct());
            running = after;
            out.add(new RiskControlDto(rc.getId(), c.getId(), c.getCode(), c.getName(),
                    c.getTreatmentMethod(), c.getReductionPct(), rc.getControlType(),
                    rc.getApplyOrder(),
                    before == null ? null : RiskCalculationService.roundScore(before),
                    after == null ? null : RiskCalculationService.roundScore(after)));
        }
        return out;
    }
}

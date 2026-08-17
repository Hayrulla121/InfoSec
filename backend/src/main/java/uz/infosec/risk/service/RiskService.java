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

import java.util.List;

/** Реестр рисков. */
@Service
public class RiskService {

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
                                     Pageable pageable) {
        return riskRepository.search(query, assetRating, threatRating, pageable)
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
        return load(riskId).getControls().stream().map(RiskService::toControlDto).toList();
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
                a.getId(), a.getCode(), a.getName(), a.getCriticalityRating(),
                t.getId(), t.getCode(), t.getDescription(), t.getTotalScore(),
                r.getName(), r.getIndicators(), r.getOwner(),
                r.getTreatmentMethod(), r.getMeasureStatus(),
                r.getImplementationDeadline(), r.getComment(),
                new RiskStage(null, r.getInherentThreatRating(),
                        r.getInherentRiskLevel(), r.getInherentRiskLabel()),
                new RiskStage(r.getCurrentScore(), r.getCurrentThreatRating(),
                        r.getCurrentRiskLevel(), r.getCurrentRiskLabel()),
                new RiskStage(r.getResidualScore(), r.getResidualThreatRating(),
                        r.getResidualRiskLevel(), r.getResidualRiskLabel()),
                controlsOfType(r, ControlType.IMPLEMENTED),
                controlsOfType(r, ControlType.PLANNED),
                r.getCreatedAt(), r.getCreatedBy(), r.getUpdatedAt(), r.getUpdatedBy());
    }

    private static List<RiskControlDto> controlsOfType(Risk risk, ControlType type) {
        return risk.getControls().stream()
                .filter(rc -> rc.getControlType() == type)
                .map(RiskService::toControlDto)
                .toList();
    }

    private static RiskControlDto toControlDto(RiskControl rc) {
        Control c = rc.getControl();
        return new RiskControlDto(rc.getId(), c.getId(), c.getCode(), c.getName(),
                c.getTreatmentMethod(), c.getReductionPct(), rc.getControlType(), rc.getApplyOrder());
    }
}

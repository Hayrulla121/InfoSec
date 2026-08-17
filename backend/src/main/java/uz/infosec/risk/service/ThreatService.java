package uz.infosec.risk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.Threat;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.ThreatRepository;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatRequest;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatResponse;

/**
 * Реестр угроз. Replaces Excel columns L-R and H: the caller supplies the five
 * DREAD scores and the server derives the rest.
 */
@Service
public class ThreatService {

    private final ThreatRepository threatRepository;
    private final RiskCalculationService calculator;
    private final RiskRecalculationService recalculation;
    private final CodeGenerator codeGenerator;

    public ThreatService(ThreatRepository threatRepository,
                         RiskCalculationService calculator,
                         RiskRecalculationService recalculation,
                         CodeGenerator codeGenerator) {
        this.threatRepository = threatRepository;
        this.calculator = calculator;
        this.recalculation = recalculation;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<ThreatResponse> search(String query, String levelLabel, Pageable pageable) {
        return threatRepository.search(query, Filters.orNull(levelLabel), pageable)
                .map(ThreatService::toDto);
    }

    @Transactional(readOnly = true)
    public ThreatResponse findById(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public ThreatResponse create(ThreatRequest request) {
        Threat threat = new Threat();
        threat.setCode(codeGenerator.next(Threat.CODE_PREFIX, threatRepository.findAllCodes()));
        apply(threat, request);
        return toDto(threatRepository.save(threat));
    }

    @Transactional
    public ThreatResponse update(Long id, ThreatRequest request) {
        Threat threat = load(id);
        apply(threat, request);
        // Dirty checking flushes on commit; no explicit save needed.
        // Changing DREAD scores changes t for every risk built on this threat,
        // so refresh them in the SAME transaction - a reader must never see a
        // threat at its new score alongside a risk at its old level.
        recalculation.recalculateForThreat(threat.getId());
        return toDto(threat);
    }

    @Transactional
    public void delete(Long id) {
        threatRepository.delete(load(id));
    }

    /**
     * The one place DREAD scores become stored values. Both create and update
     * go through it, so the computed columns can never drift from the inputs.
     */
    private void apply(Threat threat, ThreatRequest request) {
        threat.setDescription(request.description());
        threat.setDiscoverability(calculator.clampCriterion(request.discoverability()));
        threat.setRepeatability(calculator.clampCriterion(request.repeatability()));
        threat.setExploitability(calculator.clampCriterion(request.exploitability()));
        threat.setAffectedUsers(calculator.clampCriterion(request.affectedUsers()));
        threat.setDamage(calculator.clampCriterion(request.damage()));

        int total = calculator.totalScore(
                threat.getDiscoverability(), threat.getRepeatability(), threat.getExploitability(),
                threat.getAffectedUsers(), threat.getDamage());
        int rating = calculator.ratingFromScore(total);

        threat.setTotalScore(total);
        threat.setRating(rating);
        threat.setLevelLabel(calculator.threatLevelLabel(rating));
    }

    private Threat load(Long id) {
        return threatRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.threat", id));
    }

    public static ThreatResponse toDto(Threat t) {
        return new ThreatResponse(t.getId(), t.getCode(), t.getDescription(),
                t.getDiscoverability(), t.getRepeatability(), t.getExploitability(),
                t.getAffectedUsers(), t.getDamage(),
                t.getTotalScore(), t.getRating(), t.getLevelLabel(),
                t.getCreatedAt(), t.getCreatedBy(), t.getUpdatedAt(), t.getUpdatedBy());
    }
}

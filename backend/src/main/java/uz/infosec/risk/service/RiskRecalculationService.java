package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.repository.RiskRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Keeps the risks table's snapshot columns in step with everything they depend
 * on. This is the moving part that replaces Excel's automatic recalculation.
 *
 * <p>Spreadsheets recalculate the whole dependency graph on every keystroke.
 * A database does not, so each write path must explicitly say "and now refresh
 * whatever this could have changed". The fan-out methods below are that
 * statement, and they run inside the caller's transaction - so a risk is never
 * observable with stale numbers, and a failure rolls the whole thing back.
 *
 * <p>Dependency graph:
 * <pre>
 *   Threat DREAD scores ---> all risks using that threat
 *   Asset criticality   ---> all risks on that asset
 *   Control pct/type    ---> all risks linked to that control
 *   risk_controls link  ---> that one risk
 * </pre>
 */
@Service
public class RiskRecalculationService {

    private final RiskRepository riskRepository;
    private final RiskCalculationService calculator;

    public RiskRecalculationService(RiskRepository riskRepository,
                                    RiskCalculationService calculator) {
        this.riskRepository = riskRepository;
        this.calculator = calculator;
    }

    /**
     * Recomputes the three stages for one risk.
     *
     * <p>Reads through the entity graph rather than taking parameters, so it
     * cannot be called with a stale asset rating or threat score.
     */
    public void recalculate(Risk risk) {
        Asset asset = risk.getAsset();
        Threat threat = risk.getThreat();

        int a = asset.getCriticalityRating();
        BigDecimal rawScore = BigDecimal.valueOf(threat.getTotalScore());

        // 1. Inherent - the threat as scored, no controls at all.
        int inherentRating = calculator.ratingFromScore(rawScore);
        RiskLevel inherent = calculator.classify(a, inherentRating);
        risk.setInherentThreatRating(inherentRating);
        risk.setInherentRiskLevel(inherent.getLevel());
        risk.setInherentRiskLabel(inherent.getLabel());

        // 2. Current - after the IMPLEMENTED chain only (Excel col AW -> BV -> BW).
        BigDecimal currentScore = calculator.applyReductions(
                rawScore, reductions(risk.getControls(), ControlType.IMPLEMENTED));
        int currentRating = calculator.ratingFromScore(currentScore);
        RiskLevel current = calculator.classify(a, currentRating);
        risk.setCurrentScore(currentScore);
        risk.setCurrentThreatRating(currentRating);
        risk.setCurrentRiskLevel(current.getLevel());
        risk.setCurrentRiskLabel(current.getLabel());

        // 3. Residual - the PLANNED chain continues from the current score,
        //    exactly as Excel's BC column starts from AW (not from AH).
        BigDecimal residualScore = calculator.applyReductions(
                currentScore, reductions(risk.getControls(), ControlType.PLANNED));
        int residualRating = calculator.ratingFromScore(residualScore);
        RiskLevel residual = calculator.classify(a, residualRating);
        risk.setResidualScore(residualScore);
        risk.setResidualThreatRating(residualRating);
        risk.setResidualRiskLevel(residual.getLevel());
        risk.setResidualRiskLabel(residual.getLabel());
    }

    private List<BigDecimal> reductions(Collection<RiskControl> links, ControlType type) {
        return links.stream()
                .filter(link -> link.getControlType() == type)
                .map(link -> link.getControl().getReductionPct())
                .toList();
    }

    // ------------------------------------------------------------ fan-out

    @Transactional
    public void recalculateForThreat(Long threatId) {
        recalculateAll(riskRepository.findByThreatId(threatId));
    }

    @Transactional
    public void recalculateForAsset(Long assetId) {
        recalculateAll(riskRepository.findByAssetId(assetId));
    }

    @Transactional
    public void recalculateForControl(Long controlId) {
        recalculateAll(riskRepository.findByLinkedControlId(controlId));
    }

    /** Rebuilds every risk. Used by tests and after a bulk import. */
    @Transactional
    public int recalculateAll() {
        List<Risk> risks = riskRepository.findAll();
        recalculateAll(risks);
        return risks.size();
    }

    private void recalculateAll(List<Risk> risks) {
        // No explicit save: these are managed entities inside a transaction, so
        // Hibernate's dirty checking flushes the changes on commit.
        risks.forEach(this::recalculate);
    }
}

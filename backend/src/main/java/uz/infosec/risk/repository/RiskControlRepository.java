package uz.infosec.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.infosec.risk.domain.ControlType;
import uz.infosec.risk.domain.RiskControl;

public interface RiskControlRepository extends JpaRepository<RiskControl, Long> {

    /**
     * Counts links, not catalog entries: the dashboard's "% of controls
     * implemented" is about mitigation work actually in place across the risk
     * register, so the same control attached to three risks counts three times.
     */
    long countByControlType(ControlType controlType);
}

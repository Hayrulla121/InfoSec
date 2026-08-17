package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Риск-контроль - a mitigation measure. Code C1, C2...
 *
 * <p>In the workbook a control belongs to exactly one risk. Here it is a shared
 * catalog entry linked to risks through risk_controls, so the same measure can
 * be reused across many risks without copy-paste.
 */
@Entity
@Table(name = "controls")
@Getter
@Setter
@NoArgsConstructor
public class Control extends AuditableEntity {

    public static final String CODE_PREFIX = "C";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Dictionary label from TREATMENT_METHOD. */
    @Column(name = "treatment_method", nullable = false, length = 32)
    private String treatmentMethod;

    /**
     * Процент снижения риска as a share of 1, e.g. 0.20 for 20%.
     *
     * <p>BigDecimal, not double: these values are chained multiplicatively and
     * binary floating point cannot represent 0.1 or 0.2 exactly, so errors would
     * accumulate and could flip a rating across a threshold.
     */
    @Column(name = "reduction_pct", nullable = false, precision = 4, scale = 2)
    private BigDecimal reductionPct;

    /**
     * Catalog-level default for "Внедрен?". The authoritative flag for the
     * calculation is control_type on the risk_controls link, because the same
     * control may be implemented for one risk and merely planned for another.
     */
    @Column(nullable = false)
    private boolean implemented;
}

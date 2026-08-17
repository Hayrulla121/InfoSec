package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Реестр рисков - one asset x threat pair with its computed levels.
 */
@Entity
@Table(name = "risks")
@Getter
@Setter
@NoArgsConstructor
public class Risk extends AuditableEntity {

    public static final String CODE_PREFIX = "R";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threat_id", nullable = false)
    private Threat threat;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String indicators;

    @Column(name = "owner")
    private String owner;

    @Column(name = "treatment_method", length = 32)
    private String treatmentMethod;

    @Column(name = "measure_status", length = 64)
    private String measureStatus;

    @Column(name = "implementation_deadline")
    private LocalDate implementationDeadline;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * The attached controls.
     *
     * <p>cascade = ALL + orphanRemoval: links live and die with their risk and
     * have no meaning on their own. This is the one place a cascade is right -
     * we would never cascade to Control, which is a shared catalog entry.
     */
    @OneToMany(mappedBy = "risk", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("applyOrder ASC, id ASC")
    private List<RiskControl> controls = new ArrayList<>();

    // ---- computed snapshots, written only by RiskRecalculationService ----

    @Column(name = "inherent_threat_rating")
    private Integer inherentThreatRating;

    @Column(name = "inherent_risk_level")
    private Integer inherentRiskLevel;

    @Column(name = "inherent_risk_label", length = 32)
    private String inherentRiskLabel;

    @Column(name = "current_score", precision = 8, scale = 4)
    private BigDecimal currentScore;

    @Column(name = "current_threat_rating")
    private Integer currentThreatRating;

    @Column(name = "current_risk_level")
    private Integer currentRiskLevel;

    @Column(name = "current_risk_label", length = 32)
    private String currentRiskLabel;

    @Column(name = "residual_score", precision = 8, scale = 4)
    private BigDecimal residualScore;

    @Column(name = "residual_threat_rating")
    private Integer residualThreatRating;

    @Column(name = "residual_risk_level")
    private Integer residualRiskLevel;

    @Column(name = "residual_risk_label", length = 32)
    private String residualRiskLabel;
}

package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Link row: "control C is IMPLEMENTED (or PLANNED) for risk R". */
@Entity
@Table(name = "risk_controls")
@Getter
@Setter
@NoArgsConstructor
public class RiskControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_id", nullable = false)
    private Risk risk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 16)
    private ControlType controlType;

    /**
     * Display order only. The reduction chain multiplies, and multiplication
     * commutes, so this can never change a computed score.
     */
    @Column(name = "apply_order", nullable = false)
    private int applyOrder;
}

package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Реестр угроз - a threat scored with the DREAD model. Code У1, У2...
 *
 * <p>The last three fields are computed by RiskCalculationService on every save;
 * they are never set from a request body.
 */
@Entity
@Table(name = "threats")
@Getter
@Setter
@NoArgsConstructor
public class Threat extends AuditableEntity {

    public static final String CODE_PREFIX = "У";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Обнаружение (D) */
    @Column(nullable = false)
    private int discoverability;

    /** Повторение (R) */
    @Column(nullable = false)
    private int repeatability;

    /** Эксплуатирование (E) */
    @Column(nullable = false)
    private int exploitability;

    /** Масштаб (A) */
    @Column(name = "affected_users", nullable = false)
    private int affectedUsers;

    /** Ущерб (D) */
    @Column(nullable = false)
    private int damage;

    /** Computed - Excel col Q. Sum of the five, 0..25. */
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    /** Computed - Excel col R. This is the inherent {@code t}. */
    @Column(nullable = false)
    private int rating;

    /** Computed - Excel col H. */
    @Column(name = "level_label", nullable = false, length = 32)
    private String levelLabel;
}

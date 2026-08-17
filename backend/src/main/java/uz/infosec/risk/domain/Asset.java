package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Реестр ключевых ИА - a Key Information Asset. Code КИА1, КИА2...
 */
@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class Asset extends AuditableEntity {

    public static final String CODE_PREFIX = "КИА";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    /** Optional 1:1 link to the detailed system inventory. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_system_id")
    private InfoSystem infoSystem;

    /** Axborot tizimining nomi */
    @Column(nullable = false)
    private String name;

    /** Axborot tizimining ko'lami, e.g. "В масштабе республики" */
    private String scope;

    /** Qayta ishlanadigan axborot toifasi */
    @Column(name = "info_category")
    private String infoCategory;

    /** Dictionary label from ASSET_CRITICALITY: Очень низкая .. Критичная */
    @Column(nullable = false, length = 32)
    private String criticality;

    /**
     * Computed from {@link #criticality} via the dictionary - Excel column H.
     * This is the {@code a} of the a x t risk formula, so it is stored rather
     * than looked up on every matrix query.
     */
    @Column(name = "criticality_rating", nullable = false)
    private int criticalityRating;

    /** Класс защищенности: IS1-IS4 */
    @Column(name = "security_class", length = 8)
    private String securityClass;
}

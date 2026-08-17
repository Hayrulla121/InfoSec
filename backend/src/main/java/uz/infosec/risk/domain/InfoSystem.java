package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Перечень инфосистем Банка. Code ИС1, ИС2... */
@Entity
@Table(name = "info_systems")
@Getter
@Setter
@NoArgsConstructor
public class InfoSystem extends AuditableEntity {

    public static final String CODE_PREFIX = "ИС";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String hosting;

    /** "usage" is a reserved word in several SQL dialects, hence the rename. */
    @Column(name = "usage_purpose", columnDefinition = "TEXT")
    private String usagePurpose;

    @Column(name = "data_format", length = 32)
    private String dataFormat;

    @Column(length = 32)
    private String confidentiality;

    @Column(length = 8)
    private String integrity;

    @Column(length = 8)
    private String availability;

    @Column(name = "update_frequency", length = 64)
    private String updateFrequency;

    @Column(name = "users_info", columnDefinition = "TEXT")
    private String usersInfo;

    private String owner;
}

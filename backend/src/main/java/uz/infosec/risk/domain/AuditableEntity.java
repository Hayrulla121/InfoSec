package uz.infosec.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Audit columns shared by every business table.
 *
 * <p>@MappedSuperclass means "inherit these fields into the subclass's own
 * table" - it is not an entity itself and gets no table of its own.
 *
 * <p>The values are filled automatically by Spring Data's
 * AuditingEntityListener, which reads the current user from the AuditorAware
 * bean in JpaAuditingConfig. Nobody has to remember to set them.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;
}

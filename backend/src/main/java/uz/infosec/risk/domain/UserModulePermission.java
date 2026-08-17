package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row = "user X may do these four things in module Y".
 *
 * <p>Only ever created for USER accounts. ADMIN is short-circuited in
 * {@link uz.infosec.risk.security.PermissionService}, so adding a new module to
 * {@link AppModule} never requires backfilling admin grants.
 */
@Entity
@Table(name = "user_module_permissions")
@Getter
@Setter
@NoArgsConstructor
public class UserModulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FetchType.LAZY: loading a permission row must not drag the whole User in
     * with it. EAGER (the default for @ManyToOne!) is the single most common
     * cause of accidental "why did this query join six tables" surprises.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 32)
    private AppModule module;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_read", nullable = false)
    private boolean canRead = true;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    /** Translates an Action into the matching boolean column. */
    public boolean allows(Action action) {
        return switch (action) {
            case CREATE -> canCreate;
            case READ -> canRead;
            case UPDATE -> canUpdate;
            case DELETE -> canDelete;
        };
    }

    public void set(Action action, boolean value) {
        switch (action) {
            case CREATE -> canCreate = value;
            case READ -> canRead = value;
            case UPDATE -> canUpdate = value;
            case DELETE -> canDelete = value;
        }
    }
}

package uz.infosec.risk.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.repository.UserModulePermissionRepository;

import java.util.List;

/**
 * The single place that answers "may this user perform this action here?".
 * Both the aspect (enforcement) and the login response (UI hints) go through
 * it, so the backend and the frontend can never disagree about the rules.
 */
@Service
public class PermissionService {

    private final UserModulePermissionRepository permissionRepository;

    public PermissionService(UserModulePermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(AppUserDetails principal, AppModule module, Action action) {
        // ADMIN short-circuits before any query. This is why adding a new module
        // to AppModule never requires backfilling grant rows for admins.
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }
        return permissionRepository.findByUserId(principal.getId()).stream()
                .filter(p -> p.getModule() == module)
                .findFirst()
                .map(p -> p.allows(action))
                // No row for that module = no access. Default deny.
                .orElse(false);
    }

    /**
     * The user's effective grants, one entry per module, for the UI to hide
     * buttons with. Admins get an all-true grid that exists only in memory.
     */
    @Transactional(readOnly = true)
    public List<EffectivePermission> effectivePermissions(Long userId, Role role) {
        if (role == Role.ADMIN) {
            return java.util.Arrays.stream(AppModule.values())
                    .map(m -> new EffectivePermission(m, true, true, true, true))
                    .toList();
        }

        List<UserModulePermission> stored = permissionRepository.findByUserId(userId);
        return java.util.Arrays.stream(AppModule.values())
                .map(module -> stored.stream()
                        .filter(p -> p.getModule() == module)
                        .findFirst()
                        .map(p -> new EffectivePermission(module,
                                p.isCanCreate(), p.isCanRead(), p.isCanUpdate(), p.isCanDelete()))
                        // Modules with no row at all are reported as all-false.
                        .orElse(new EffectivePermission(module, false, false, false, false)))
                .toList();
    }

    public record EffectivePermission(AppModule module,
                                      boolean canCreate,
                                      boolean canRead,
                                      boolean canUpdate,
                                      boolean canDelete) {
    }
}

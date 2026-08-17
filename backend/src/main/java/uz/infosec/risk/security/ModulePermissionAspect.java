package uz.infosec.risk.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.infosec.risk.error.ModuleAccessDeniedException;

/**
 * Enforces {@link RequireModulePermission}.
 *
 * <p>How it works: Spring wraps every bean carrying an advised method in a
 * proxy. When something calls that method, the proxy runs this advice first and
 * only then the real body. Because the check happens outside the method, it
 * cannot be forgotten inside it.
 *
 * <p><b>The catch every Spring developer meets once:</b> proxies only intercept
 * calls that arrive <i>through</i> them. If a controller method calls another
 * annotated method on {@code this}, the proxy is bypassed and no check runs.
 * So annotate the entry points - the controller methods - not internal helpers.
 */
@Aspect
@Component
public class ModulePermissionAspect {

    private final PermissionService permissionService;

    public ModulePermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Binding the annotation as a parameter named {@code requirement} tells
     * Spring AOP: match any method annotated with this type, and hand me the
     * annotation instance so I can read module() and action().
     */
    @Before("@annotation(requirement)")
    public void check(RequireModulePermission requirement) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AccessDeniedException("Authentication required");
        }

        if (!permissionService.isAllowed(principal, requirement.module(), requirement.action())) {
            // Names the missing grant, translated at the edge.
            throw new ModuleAccessDeniedException(requirement.module(), requirement.action());
        }
    }
}

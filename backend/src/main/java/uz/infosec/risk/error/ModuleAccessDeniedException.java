package uz.infosec.risk.error;

import org.springframework.security.access.AccessDeniedException;
import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;

/**
 * "You may not do X in module Y" - a denial that names what was missing.
 *
 * <p>Extends Spring Security's {@link AccessDeniedException} so the existing
 * filter chain still treats it as a denial, while carrying a message code so
 * the user is told which grant they lack, in their own language. Telling an
 * authenticated colleague exactly what to ask their administrator for is worth
 * more here than withholding it: the permission grid is not a secret, and a
 * bare "Access denied" turns into a support ticket.
 */
public class ModuleAccessDeniedException extends AccessDeniedException
        implements LocalizedException {

    private final transient Object[] messageArgs;

    public ModuleAccessDeniedException(AppModule module, Action action) {
        super("Missing permission %s on module %s".formatted(action, module));
        // Nested keys, resolved by GlobalExceptionHandler before substitution.
        this.messageArgs = new Object[]{
                "permission.action." + action.name(),
                "permission.module." + module.name()};
    }

    @Override
    public String getMessageCode() {
        return "error.missingPermission";
    }

    @Override
    public Object[] getMessageArgs() {
        return messageArgs;
    }
}

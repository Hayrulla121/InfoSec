package uz.infosec.risk.security;

import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permission a controller method requires, e.g.
 * {@code @RequireModulePermission(module = AppModule.RISKS, action = Action.UPDATE)}.
 *
 * <p>Enforced by {@link ModulePermissionAspect}. RetentionPolicy.RUNTIME is
 * essential - annotations default to CLASS retention, which is discarded before
 * runtime, and the aspect would silently never fire.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireModulePermission {

    AppModule module();

    Action action();
}

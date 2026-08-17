package uz.infosec.risk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on @PreAuthorize / @PostAuthorize support.
 *
 * <p>Without this annotation those checks are silently ignored - the code
 * compiles, the endpoint works, and it is simply unprotected. That failure mode
 * is invisible, which is why it lives in its own clearly named class.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}

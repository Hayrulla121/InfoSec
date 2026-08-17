package uz.infosec.risk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.infosec.risk.security.AppUserDetails;

import java.util.Optional;

/**
 * Wires the created_by / updated_by columns to the logged-in user.
 *
 * <p>@EnableJpaAuditing activates the listener on AuditableEntity; without it
 * the annotations are inert and every audit column stays NULL.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUserDetails principal) {
                return Optional.of(principal.getUsername());
            }
            // Migrations and tests run without a logged-in user.
            return Optional.of("system");
        };
    }
}

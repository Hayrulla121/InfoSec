package uz.infosec.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the app.jwt.* block in application.yml.
 *
 * <p>Preferred over scattering @Value("${app.jwt.secret}") through the code:
 * the settings are validated and bound once, at startup, in one place - so a
 * typo in the YAML fails immediately rather than at first use.
 *
 * <p>A Java record works because binding is constructor-based. Registered via
 * @EnableConfigurationProperties in SecurityConfig.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}

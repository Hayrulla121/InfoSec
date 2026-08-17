package uz.infosec.risk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The React dev server runs on http://localhost:5173 and the API on :8080.
 * Different port = different origin, so the browser refuses the request unless
 * the server explicitly opts in. That opt-in is CORS.
 *
 * We expose a CorsConfigurationSource bean rather than using
 * WebMvcConfigurer#addCorsMappings, because Spring Security's filter chain runs
 * *before* Spring MVC. Security picks this bean up automatically once
 * http.cors(...) is enabled in SecurityConfig - otherwise the browser's
 * preflight OPTIONS request would be rejected with 401 before MVC ever sees it.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        // Lets the browser read the Authorization header we will send back later.
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}

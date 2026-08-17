package uz.infosec.risk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uz.infosec.risk.error.ApiError;
import uz.infosec.risk.security.JwtAuthenticationFilter;

/**
 * The real filter chain (Phase 1 replaced the permit-all placeholder).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * BCrypt is deliberately SLOW (cost factor 10 = 2^10 internal rounds) and
     * salts every hash. That is what makes stolen hashes impractical to brute
     * force. Never replace this with MD5/SHA - those are fast by design, which
     * is exactly wrong for passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring assembles this from our AppUserDetailsService + PasswordEncoder.
     * AuthController uses it so that login goes through the same, well-tested
     * code path as any other Spring Security authentication.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // No HttpSession, no JSESSIONID cookie. Every request must carry
                // its own proof of identity. This is what "stateless" means and
                // why the API scales horizontally without sticky sessions.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Rules are evaluated top-down; the first match wins,
                        // so the specific public routes must precede anyRequest().
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())

                // Our filter must populate the SecurityContext BEFORE the
                // authorisation rules above are evaluated.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        // Not authenticated at all -> 401
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, 401, "Authentication required"))
                        // Authenticated but not permitted -> 403
                        .accessDeniedHandler((request, response, deniedException) ->
                                writeError(response, 403, "Access denied")))

                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * Errors raised inside the filter chain never reach @RestControllerAdvice,
     * because they happen before the DispatcherServlet. We serialise the same
     * ApiError shape by hand so the frontend sees one consistent contract.
     */
    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, message));
    }
}

package uz.infosec.risk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request: reads the Authorization header, validates the token,
 * and puts the resulting authentication into the SecurityContext so that
 * downstream rules (and our permission aspect) can see who is calling.
 *
 * <p>If there is no token, or it is invalid, this filter does NOT reject the
 * request - it simply leaves the context empty and moves on. Rejection is the
 * job of the authorisation rules later in the chain. Keeping those two
 * responsibilities separate is what lets /api/auth/login stay public while
 * everything else is protected, with no special-casing here.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIX.length());

            jwtService.parse(token).ifPresent(claims -> {
                // We re-load the user from the database on every request instead
                // of trusting the token's claims blindly. That costs one indexed
                // query, and buys immediate revocation: the moment an admin
                // deactivates an account, its still-unexpired tokens stop
                // working. A purely stateless check could not do that.
                try {
                    AppUserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
                    if (user.isEnabled()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (RuntimeException ignored) {
                    // User deleted since the token was issued: stay anonymous.
                }
            });
        }

        chain.doFilter(request, response);
    }
}

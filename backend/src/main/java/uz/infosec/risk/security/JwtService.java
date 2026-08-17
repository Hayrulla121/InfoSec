package uz.infosec.risk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import uz.infosec.risk.config.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and verifies JSON Web Tokens.
 *
 * <p>A JWT is three base64 segments joined by dots: header.payload.signature.
 * The payload is only ENCODED, not encrypted - anyone can read it. Never put a
 * secret in a claim. The signature is what makes it trustworthy: it is an
 * HMAC-SHA256 of header+payload using our server-side key, so a tampered
 * payload no longer matches and verification fails.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "uid";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        // hmacShaKeyFor rejects keys shorter than 256 bits, so a weak secret
        // fails loudly at startup rather than silently weakening every token.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expirationMinutes = properties.expirationMinutes();
    }

    public String generateToken(AppUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature and expiry, then returns the claims.
     * Empty means "reject this request" - we never distinguish *why* a token is
     * invalid to the caller.
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            // Covers bad signature, malformed token, and expiry.
            return Optional.empty();
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}

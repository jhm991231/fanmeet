package dev.fanmeet.auth;

import dev.fanmeet.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long accessValiditySeconds;
    private final long refreshValiditySeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-validity-seconds:1800}") long accessValiditySeconds,
            @Value("${jwt.refresh-validity-seconds:1209600}") long refreshValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValiditySeconds = accessValiditySeconds;
        this.refreshValiditySeconds = refreshValiditySeconds;
    }

    public String createAccessToken(Long userId, Role role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(new Date())
                .expiration(expirationFrom(accessValiditySeconds))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(expirationFrom(refreshValiditySeconds))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Role parseRole(String token) {
        return Role.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date expirationFrom(long validitySeconds) {
        return new Date(System.currentTimeMillis() + validitySeconds * 1000);
    }
}

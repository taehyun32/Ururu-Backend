package com.ururulab.ururu.auth.jwt;

import com.ururulab.ururu.auth.exception.InvalidJwtTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 컴포넌트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_JTI = "jti"; // jti 고유 토큰ID (표준 클레임)

    private final JwtProperties jwtProperties;

    public String generateAccessToken(final Long memberId, final String email, final String role) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        return createToken(memberId, email, role, TokenType.ACCESS, jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(final Long memberId) {
        return createToken(memberId, null, null, TokenType.REFRESH, jwtProperties.getRefreshTokenExpiry());
    }

    public Long getMemberId(final String token) {
        final Claims claims = parseToken(token);
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidJwtTokenException("Invalid member ID format in token", e);
        }
    }

    public String getEmail(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public String getRole(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    public boolean validateToken(final String token) {
        try {
            parseToken(token);
            return true;
        } catch (final JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(final String token) {
        try {
            final Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (final ExpiredJwtException e) {
            return true;
        } catch (final JwtException e) {
            log.debug("JWT parsing failed: {}", e.getMessage());
            throw new InvalidJwtTokenException("Failed to parse JWT token", e);
        }
    }

    public Long getAccessTokenExpiry() {
        return jwtProperties.getAccessTokenExpiry();
    }

    public long getRefreshTokenExpirySeconds() {
        return jwtProperties.getRefreshTokenExpiry();
    }

    public boolean isRefreshToken(final String token) {
        final Claims claims = parseToken(token);
        return TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(final String token) {
        final Claims claims = parseToken(token);
        return TokenType.ACCESS.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String getTokenId(final String token) {
        final Claims claims = parseToken(token);
        return claims.getId();
    }

    public long getRemainingExpiry(final String token) {
        final Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return Math.max((expiration.getTime() - now) / 1000, 0);
    }

    private String createToken(final Long memberId, final String email, final String role,
                               final TokenType type, final long expirySeconds) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + expirySeconds * 1000);

        final JwtBuilder builder = Jwts.builder()
                .subject(memberId.toString())
                .claim(CLAIM_TYPE, type.name())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString()) // jti 고유값 자동 부여
                .signWith(getSecretKey());

        if (email != null) {
            builder.claim(CLAIM_EMAIL, email);
        }
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }

        return builder.compact();
    }

    private Claims parseToken(final String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public enum TokenType {
        ACCESS, REFRESH
    }
}

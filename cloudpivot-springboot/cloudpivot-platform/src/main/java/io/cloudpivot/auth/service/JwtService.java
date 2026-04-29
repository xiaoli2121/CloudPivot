package io.cloudpivot.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenTtl;
    private final long refreshTokenTtl;
    private final long clockSkewSeconds;

    public JwtService(
            @Value("${cloudpivot.auth.jwt.secret}") String secret,
            @Value("${cloudpivot.auth.jwt.issuer}") String issuer,
            @Value("${cloudpivot.auth.jwt.access-token-ttl}") long accessTokenTtl,
            @Value("${cloudpivot.auth.jwt.refresh-token-ttl}") long refreshTokenTtl,
            @Value("${cloudpivot.auth.jwt.clock-skew-seconds:60}") long clockSkewSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(secret));
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public TokenPair issueTokens(long userId, String userName, List<String> roles, long authVersion) {
        return issueTokens(userId, userName, roles, authVersion, UUID.randomUUID().toString());
    }

    public TokenPair issueTokens(long userId, String userName, List<String> roles, long authVersion, String sessionId) {
        Instant now = Instant.now();
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();
        Instant accessExpiry = now.plusSeconds(accessTokenTtl);
        Instant refreshExpiry = now.plusSeconds(refreshTokenTtl);

        String accessToken = Jwts.builder()
                .id(accessTokenId)
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiry))
                .claim("sid", sessionId)
                .claim("type", "access")
                .claim("name", userName)
                .claim("roles", roles)
                .claim("authVersion", authVersion)
                .signWith(signingKey)
                .compact();

        String refreshToken = Jwts.builder()
                .id(refreshTokenId)
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiry))
                .claim("sid", sessionId)
                .claim("type", "refresh")
                .claim("authVersion", authVersion)
                .signWith(signingKey)
                .compact();

        return new TokenPair(accessToken, refreshToken, accessTokenTtl, sessionId, accessTokenId, refreshTokenId, refreshExpiry);
    }

    public AccessClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Access token is invalid.");
        }
        return new AccessClaims(
                claims.getId(),
                claims.get("sid", String.class),
                Long.parseLong(claims.getSubject()),
                claims.get("name", String.class),
                claims.get("roles", List.class),
                claims.get("authVersion", Number.class).longValue(),
                claims.getExpiration().toInstant());
    }

    public RefreshClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Refresh token is invalid.");
        }
        return new RefreshClaims(
                claims.getId(),
                claims.get("sid", String.class),
                Long.parseLong(claims.getSubject()),
                claims.get("authVersion", Number.class).longValue(),
                claims.getExpiration().toInstant());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(clockSkewSeconds)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("Token is invalid or expired.");
        }
    }

    private byte[] resolveSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String sessionId,
            String accessTokenId,
            String refreshTokenId,
            Instant refreshExpiresAt) {
    }

    public record AccessClaims(
            String tokenId,
            String sessionId,
            long userId,
            String userName,
            List<String> roles,
            long authVersion,
            Instant expiresAt) {
    }

    public record RefreshClaims(
            String tokenId,
            String sessionId,
            long userId,
            long authVersion,
            Instant expiresAt) {
    }
}

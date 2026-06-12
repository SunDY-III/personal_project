package com.smartticket.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    private final SecretKey key;
    private final long expireMs;

    public JwtUtils(@org.springframework.beans.factory.annotation.Value("${smartticket.jwt.secret:smartticket-jwt-secret-key-2026-min-256-bits!!}") String secret,
                    @org.springframework.beans.factory.annotation.Value("${smartticket.jwt.expire-ms:7200000}") long expireMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireMs;
    }

    public String generate(Long userId, String username, String role) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expireMs))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(Claims claims) { return Long.parseLong(claims.getSubject()); }
    public String getRole(Claims claims) { return claims.get("role", String.class); }
}

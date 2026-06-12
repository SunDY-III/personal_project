package com.smartticket.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    private final SecretKey key = Keys.hmacShaKeyFor("smartticket-jwt-secret-key-2026-min-256-bits!!".getBytes(StandardCharsets.UTF_8));
    private final long expireMs = 7_200_000; // 2h

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

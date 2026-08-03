package com.smartqueue.queuemanager.security;

import com.smartqueue.queuemanager.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry.ms}")
    private long expiryMs;

    private Key getSigningKey() {
        // If secret is plain text (not Base64), use it as bytes directly
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate a JWT token for a given user
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract email (subject) from token
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Extract userId from token
    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    // Validate token — checks signature + expiry
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
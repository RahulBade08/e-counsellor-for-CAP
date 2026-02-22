package com.ecounsellor.backend.admin.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * UPDATED JwtUtil — now embeds a "role" claim in the token.
 * Backward compatible: existing admin tokens still work because
 * extractUsername() and token validation are unchanged.
 *
 * REPLACE your existing JwtUtil.java with this file.
 */
@Component
public class JwtUtil {

    // Must be 32+ characters
    private static final String SECRET = "ecounsellor-secret-key-ecounsellor-secret-key";

    // Token valid for 30 days (students stay logged in)
    private static final long EXPIRY_MS = 30L * 24 * 60 * 60 * 1000;

    // ── Generate token WITH role ───────────────────────────────────────────────
    public String generateToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)            // NEW — embed role
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Backward-compatible: generate token without explicit role (ADMIN default) ─
    public String generateToken(String subject) {
        return generateToken(subject, "ADMIN");
    }

    // ── Extract subject (phone for students, username for admin) ─────────────
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // ── Extract role ──────────────────────────────────────────────────────────
    public String extractRole(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "ADMIN";  // backward compat
    }

    // ── Validate (throws exception if invalid/expired) ────────────────────────
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

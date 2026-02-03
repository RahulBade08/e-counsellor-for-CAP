package com.ecounsellor.backend.admin.util;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final String SECRET =
        "ecounsellor-secret-key-ecounsellor-secret-key"; 
        // must be 32+ chars

    public String generateToken(String username) {

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(
                    new Date(System.currentTimeMillis() + 86400000)
                )
                .signWith(
                    Keys.hmacShaKeyFor(SECRET.getBytes()),
                    SignatureAlgorithm.HS256
                )
                .compact();
    }

    public String extractUsername(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(
                    Keys.hmacShaKeyFor(SECRET.getBytes())
                )
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}

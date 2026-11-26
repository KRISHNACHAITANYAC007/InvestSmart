package com.example.investsmart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    public String sessionToken(String email,String role,String sessionId,java.time.Instant expires) {
        return Jwts.builder().subject(email).id(sessionId).claim("role",role).issuedAt(new Date()).expiration(Date.from(expires)).signWith(getSigningKey()).compact();
    }
    public String sessionId(String token) { return extractAllClaims(token).getId(); }

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    private Claims extractAllClaims(
            String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.example.confdoc.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import com.example.confdoc.model.Department;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.confdoc.service.KeyStoreService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

    private final KeyStoreService keyStoreService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public JwtUtil(KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    public String generateToken(String username, String role, Department department) throws Exception {
        PrivateKey key = keyStoreService.getPrivateKey();
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("department", department)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.RS256)
                .compact();
    }

    public io.jsonwebtoken.Claims parseToken(String jwt) throws Exception {
        PublicKey pub = keyStoreService.getPublicKey();
        return Jwts.parserBuilder()
                .setSigningKey(pub)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
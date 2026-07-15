// ================================
// JwtUtil.java
// ================================

package com.code.back_end.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;

import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // =========================
    // SECRET KEY
    // =========================

    private final Key key;

    public JwtUtil(
            @Value("${jwt.secret}")
            String secret
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and contain at least 32 characters."
            );
        }

        this.key =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );
    }

    // =========================
    // GENERATE TOKEN
    // =========================

    public String generateToken(
            String username
    ) {

        return generateToken(
                username,
                null
        );
    }

    public String generateToken(
            String username,
            String role
    ) {

        var builder = Jwts.builder()

                .setSubject(username)

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000 * 60 * 60 * 24
                        )
                )

                .signWith(
                        key,
                        SignatureAlgorithm.HS256
                );

        if (role != null) {
            builder.claim(
                    "role",
                    role
            );
        }

        return builder.compact();
    }

    // =========================
    // EXTRACT USERNAME
    // =========================

    public String extractUsername(
            String token
    ) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    // =========================
    // EXTRACT EXPIRATION
    // =========================

    public Date extractExpiration(
            String token
    ) {

        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    public String extractRole(
            String token
    ) {

        return extractClaim(
                token,
                claims -> claims.get(
                        "role",
                        String.class
                )
        );
    }

    // =========================
    // EXTRACT CLAIM
    // =========================

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims =
                extractAllClaims(token);

        return resolver.apply(claims);
    }

    // =========================
    // EXTRACT ALL CLAIMS
    // =========================

    private Claims extractAllClaims(
            String token
    ) {

        return Jwts.parserBuilder()

                .setSigningKey(key)

                .build()

                .parseClaimsJws(token)

                .getBody();
    }

    // =========================
    // CHECK EXPIRATION
    // =========================

    private boolean isTokenExpired(
            String token
    ) {

        return extractExpiration(token)
                .before(new Date());
    }

    // =========================
    // VALIDATE TOKEN
    // =========================

   public boolean validateToken(
        String token,
        UserDetails userDetails
) {

    final String username =
            extractUsername(token);

    return username.equals(
            userDetails.getUsername()
    ) && !isTokenExpired(token);
}

// =========================
// IS TOKEN VALID
// =========================
public boolean isTokenValid(
        String token,
        String username
) {
    return extractUsername(token)
            .equals(username)
            && !isTokenExpired(token);
}
    
}

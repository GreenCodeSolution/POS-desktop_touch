package com.devstack.pos.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.devstack.pos.view.tm.TokenRequest;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.refresh.validity}")
    private int refreshValidity;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.validity}")
    private int jwtValidity;

    public String createJwtToken(TokenRequest tokenRequest) {
        log.info("Creating JWT token for user: {} with roles: {}", tokenRequest.getUsername(), tokenRequest.getRoles());
        String token = JWT.create()
                .withSubject(tokenRequest.getUsername())
                .withClaim("roles", tokenRequest.getRoles() != null ? tokenRequest.getRoles() : java.util.List.of())
                .withClaim("permissions",
                        tokenRequest.getPermissions() != null ? tokenRequest.getPermissions() : java.util.List.of())
                .withIssuedAt(Date.from(tokenRequest.getNow().atZone(ZoneId.systemDefault()).toInstant()))
                .withIssuer("GreenCodeSolution")
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtValidity * 1000L))
                .sign(Algorithm.HMAC512(jwtSecret.getBytes()));
        log.info("JWT token created successfully");
        return token;
    }

    public String createRefreshToken(TokenRequest tokenRequest) {
        return JWT.create()
                .withSubject(tokenRequest.getUsername())
                .withClaim("roles", tokenRequest.getRoles() != null ? tokenRequest.getRoles() : java.util.List.of())
                .withClaim("permissions",
                        tokenRequest.getPermissions() != null ? tokenRequest.getPermissions() : java.util.List.of())
                .withIssuedAt(Date.from(tokenRequest.getNow().atZone(ZoneId.systemDefault()).toInstant()))
                .withIssuer("GreenCodeSolution")
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshValidity * 1000L))
                .sign(Algorithm.HMAC512(jwtSecret.getBytes()));
    }

    public String getEmailFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        java.util.List<String> roles = getRolesFromToken(token);
        return roles.isEmpty() ? null : roles.get(0);
    }

    public java.util.List<String> getRolesFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            java.util.List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            return roles != null ? roles : java.util.List.of();
        } catch (Exception e) {
            log.error("Failed to extract roles from token: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    /**
     * Get permissions from token
     */
    public java.util.List<String> getPermissionsFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            java.util.List<String> permissions = decodedJWT.getClaim("permissions").asList(String.class);
            return permissions != null ? permissions : java.util.List.of();
        } catch (Exception e) {
            log.error("Failed to extract permissions from token: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the expiration date from a JWT token
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getExpiresAt();
        } catch (Exception e) {
            log.error("Failed to get expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a JWT token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            if (expirationDate == null) {
                return true;
            }
            return expirationDate.before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

}

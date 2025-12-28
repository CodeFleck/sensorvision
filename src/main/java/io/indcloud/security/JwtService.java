package io.indcloud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    // For refresh tokens (7 days)
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L;

    // For "Remember Me" refresh tokens (30 days)
    private static final long REMEMBER_ME_EXPIRATION_MS = 2592000000L;

    private SecretKey getSigningKey() {
        // Ensure the key is at least 256 bits for HS256
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token
     */
    public String generateAccessToken(UserDetails userDetails, Long userId, Long organizationId) {
        return generateToken(userDetails, userId, organizationId, jwtExpirationMs, "access");
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(UserDetails userDetails, Long userId, Long organizationId) {
        return generateToken(userDetails, userId, organizationId, REFRESH_TOKEN_EXPIRATION_MS, "refresh");
    }

    /**
     * Generate refresh token with Remember Me (extended expiration)
     */
    public String generateRememberMeRefreshToken(UserDetails userDetails, Long userId, Long organizationId) {
        return generateToken(userDetails, userId, organizationId, REMEMBER_ME_EXPIRATION_MS, "refresh");
    }

    /**
     * Generate JWT token with custom expiration
     */
    private String generateToken(UserDetails userDetails, Long userId, Long organizationId,
                                  long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("organizationId", organizationId);
        claims.put("roles", roles);
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Get user ID from JWT token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    /**
     * Get organization ID from JWT token
     */
    public Long getOrganizationIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("organizationId", Long.class);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get roles from JWT token
     *
     * @param token the JWT token
     * @return list of role names (e.g., ["ROLE_USER", "ROLE_DEVELOPER"])
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("roles", List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Check if token has a specific role
     *
     * @param token the JWT token
     * @param role the role to check (e.g., "ROLE_DEVELOPER")
     * @return true if the token has the specified role
     */
    public boolean hasRole(String token, String role) {
        List<String> roles = getRolesFromToken(token);
        return roles != null && roles.contains(role);
    }
}

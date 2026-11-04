package com.akt.banking_v1.service;

import com.akt.banking_v1.config.SecurityProperties;
import com.akt.banking_v1.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;
    private final SecretKey signingKey;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.signingKey = getSigningKey(securityProperties);
    }

    private SecretKey getSigningKey(SecurityProperties securityProperties) {

        byte[] keyBytes = Decoders.BASE64.decode(securityProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {

        Instant now = Instant.now();
        Instant expirationDate = now.plus(securityProperties.getSecretKeyExpiration());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        Map<String, Object> claims = (extraClaims != null) ? new HashMap<>(extraClaims) : new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuer(securityProperties.getIssuer())
                .audience().add(securityProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationDate))
                .signWith(signingKey)
                .compact();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public Role extractRole(Claims claims) {

        String role = (String) claims.get("role");
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        return Role.valueOf(role);
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(securityProperties.getIssuer())
                .requireAudience(securityProperties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

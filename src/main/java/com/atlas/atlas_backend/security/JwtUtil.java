package com.atlas.atlas_backend.security;

import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:ThisIsAVerySecretKeyThatShouldBeStoredInEnvironmentVariablesAndNotHere}")
    private String SECRET_KEY;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, new HashMap<>());
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseAndValidateClaims(String token) {
        return extractAllClaims(token);
    }

    public Integer extractTokenVersion(Claims claims) {
        Object version = claims.get("tokenVersion");
        if (version instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public Integer extractTokenVersion(String token) {
        return extractTokenVersion(extractAllClaims(token));
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean validateToken(String token, UserDetails userDetails, Integer expectedTokenVersion) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        Integer tokenVersion = extractTokenVersion(claims);
        return username != null
                && username.equals(userDetails.getUsername())
                && userDetails.isEnabled()
                && expectedTokenVersion != null
                && expectedTokenVersion.equals(tokenVersion)
                && claims.getExpiration() != null
                && !claims.getExpiration().before(new Date());
    }
}

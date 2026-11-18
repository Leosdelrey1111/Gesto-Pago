package com.proyecto.servicios.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key key;

    /**
     * La clave de firma se inyecta desde la variable de entorno JWT_SECRET (Base64, minimo 256 bits).
     * No tiene valor por defecto: sin ella la aplicacion no arranca.
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secretBase64) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    public String generateToken(String username, String sessionId, String deviceId) {
        return generateToken(username, sessionId, deviceId, "CLIENTE");
    }

    public String generateToken(String username, String sessionId, String deviceId, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        claims.put("deviceId", deviceId);
        // Claims estándar tipo AWS Cognito / Keycloak (RBAC + Scopes de Lectura y Escritura)
        java.util.List<String> roles = "ADMIN".equalsIgnoreCase(rol)
                ? java.util.List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_CLIENTE")
                : java.util.List.of("ROLE_USER", "ROLE_CLIENTE");
        claims.put("roles", roles);
        claims.put("scope", java.util.List.of("SCOPE_READ", "SCOPE_WRITE"));
        claims.put("cognito:groups", java.util.List.of("ClientesFintech"));
        return createToken(claims, username);
    }

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_PRE_AUTH = "PRE_AUTH";
    public static final String CLAIM_CLIENT_ID = "client_id";
    public static final long CLIENT_TOKEN_TTL_SECONDS = 7200L;
    private static final long PRE_AUTH_TTL_MILLIS = 5 * 60 * 1000L;

    /** Token de aplicación (client_credentials): solo lleva los scopes concedidos, sin roles de usuario. */
    public String generateClientToken(String clientId, java.util.List<String> scopes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_CLIENT_ID, clientId);
        claims.put("scope", scopes.stream().map(s -> "SCOPE_" + s).toList());
        return Jwts.builder().setClaims(claims).setSubject("client:" + clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + CLIENT_TOKEN_TTL_SECONDS * 1000))
                .signWith(key).compact();
    }

    /** Token del paso intermedio del 2FA: sin roles ni scopes; solo sirve para completar la verificación. */
    public String generatePreAuthToken(String username, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_PRE_AUTH);
        claims.put("deviceId", deviceId);
        return Jwts.builder().setClaims(claims).setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + PRE_AUTH_TTL_MILLIS))
                .signWith(key).compact();
    }

    /** @return true solo si el token está firmado, vigente, es PRE_AUTH y pertenece al usuario indicado. */
    public boolean isValidPreAuthToken(String token, String username) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            return TYPE_PRE_AUTH.equals(claims.get(CLAIM_TYPE, String.class))
                    && username != null && username.equalsIgnoreCase(claims.getSubject());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public String extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", String.class));
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas
                .signWith(key).compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

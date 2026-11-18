package com.proyecto.servicios.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Decodifica tokens JWT verificando SIEMPRE la firma HMAC con la clave del servidor.
 * Un token con firma inválida, sin firma o manipulado se rechaza: nunca se leen claims sin verificar.
 */
@Component
public class KeycloakJwtDecoder {

    private final Key key;

    public KeycloakJwtDecoder(@Value("${app.jwt.secret}") String secretBase64) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public Claims decodeClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new TokenValidationException("Token inválido", e);
        }
    }
}

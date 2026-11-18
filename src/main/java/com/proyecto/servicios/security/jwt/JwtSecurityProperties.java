package com.proyecto.servicios.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtSecurityProperties {

    /**
     * Clave secreta para firma HMAC-SHA local (Base64).
     */
    private String secretKey;

    /**
     * Emisor del token (Issuer).
     */
    private String issuer = "gesto-pagos-auth";

    /**
     * Tiempo de expiración del token en milisegundos (10 horas por defecto).
     */
    private long expirationMs = 36000000L;

    /**
     * URL del servidor Keycloak (ej. http://localhost:8081).
     */
    private String keycloakServerUrl = "http://localhost:8081";

    /**
     * Realm de Keycloak.
     */
    private String keycloakRealm = "master";
}


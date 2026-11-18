package com.proyecto.servicios.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private List<String> publicPaths = new ArrayList<>(List.of(
            "/",
            "/auth/login",
            "/auth/2fa/validar",
            "/auth/login-biometrico",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/personas",
            "/personasActualiza",
            "/personasElimina",
            "/error"
    ));

    private boolean enableSingleSession = true;
    private long sessionTimeoutMinutes = 120;
}


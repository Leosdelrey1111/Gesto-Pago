package com.proyecto.servicios.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Boolean requiere2FA;
    private String preAuthToken;
    private String mensaje;
    private String otpAuthUrl;
    private String totpSecret;
    private Boolean primerRegistro2FA;
    private String telefonoEnmascarado;
    private String correoEnmascarado;

    public AuthResponse(String token) {
        this.token = token;
        this.requiere2FA = false;
    }
}

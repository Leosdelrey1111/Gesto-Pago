package com.proyecto.servicios.model.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BiometricAuthRequest {
    
    @NotBlank(message = "El correo es obligatorio")
    private String correo;
    
    @NotBlank(message = "El token biométrico es obligatorio")
    private String biometricToken;
}

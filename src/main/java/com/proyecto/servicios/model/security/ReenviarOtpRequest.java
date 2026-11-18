package com.proyecto.servicios.model.security;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReenviarOtpRequest {

    @NotBlank(message = "El correo es obligatorio")
    private String correo;
}

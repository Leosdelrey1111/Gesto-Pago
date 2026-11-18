package com.proyecto.servicios.model.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPinRequest {

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe contener exactamente 4 dígitos numéricos")
    private String pin;
}


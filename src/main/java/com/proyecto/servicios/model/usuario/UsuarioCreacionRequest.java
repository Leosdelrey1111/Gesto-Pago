package com.proyecto.servicios.model.usuario;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreacionRequest {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico debe ser válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).*$",
        message = "La contraseña debe contener al menos una letra mayúscula, una minúscula, un número y un caracter especial (@$!%*?&._-)"
    )
    private String password;

    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long clienteId;

    @Builder.Default
    private Boolean activo = true;
}

package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialTransaccionesRequestDTO {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9-]{1,50}$", message = "El numero de cuenta tiene un formato invalido")
    private String numeroCuenta;

    @Min(value = 1, message = "El limite minimo es 1")
    @Max(value = 100, message = "El limite maximo es 100")
    private Integer limite;
}
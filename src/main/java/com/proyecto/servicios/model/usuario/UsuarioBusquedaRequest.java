package com.proyecto.servicios.model.usuario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioBusquedaRequest {

    @Positive(message = "El ID debe ser un número positivo")
    private Long id;

    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long clienteId;

    private String correo;

    private Boolean activo;

    private OffsetDateTime fechaCreacionDesde;

    private OffsetDateTime fechaCreacionHasta;

    @Builder.Default
    @Min(value = 0, message = "El número de página no puede ser negativo")
    private Integer pagina = 0;

    @Builder.Default
    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    private Integer tamanio = 10;

    @Builder.Default
    private String ordenarPor = "id";

    @Builder.Default
    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "La dirección de ordenamiento debe ser ASC o DESC")
    private String direccion = "ASC";
}

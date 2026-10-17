package com.proyecto.servicios.model.usuario;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResponseDTO {
    private Long id;
    private String correo;
    private Boolean activo;
    private Long clienteId;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}

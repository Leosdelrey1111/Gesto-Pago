package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> {
    private Integer codigo;
    private Integer status;
    private String mensaje;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private T data;
    private Object errores;
}

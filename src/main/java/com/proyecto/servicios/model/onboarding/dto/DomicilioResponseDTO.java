package com.proyecto.servicios.model.onboarding.dto;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioResponseDTO {
    private Long id;
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private EstadoMexico estado;
    private Integer codigoPostal;
    private Pais pais;
}

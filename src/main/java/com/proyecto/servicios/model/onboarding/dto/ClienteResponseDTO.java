package com.proyecto.servicios.model.onboarding.dto;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private Sexo sexo;
    private Nacionalidad nacionalidad;
    private EstadoCivil estadoCivil;
    private String correoElectronico;
    private Integer ladaMovil;
    private Long telefonoMovil;
    private Integer ladaAlternativo;
    private Long telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private Boolean bloqueado;
    private TipoBaja tipoBaja;
    private OffsetDateTime fechaCreacion;
    private DomicilioResponseDTO domicilio;
}

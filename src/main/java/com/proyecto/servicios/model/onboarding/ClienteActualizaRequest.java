package com.proyecto.servicios.model.onboarding;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;

@Data
public class ClienteActualizaRequest {
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private Sexo sexo;
    private Nacionalidad nacionalidad;
    private EstadoCivil estadoCivil;
    
    // Datos Contacto
    private String correoElectronico;
    private Integer ladaMovil;
    private Long telefonoMovil;
    private Integer ladaAlternativo;
    private Long telefonoAlternativo;
    
    // Domicilio
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private EstadoMexico estado;
    private Integer codigoPostal;
    private Pais pais;
    
    // Laboral
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
}

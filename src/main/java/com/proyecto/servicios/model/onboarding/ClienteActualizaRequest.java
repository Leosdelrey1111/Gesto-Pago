package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteActualizaRequest {

    // --- Datos Personales ---
    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @Past(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    private Sexo sexo;

    private Nacionalidad nacionalidad;

    private EstadoCivil estadoCivil;

    // --- Datos de Contacto ---
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;

    @Max(value = 999, message = "La lada debe tener máximo 3 dígitos")
    private Integer ladaMovil;

    @Min(value = 1000000000L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono móvil no puede exceder 10 dígitos")
    private Long telefonoMovil;

    @Max(value = 999, message = "La lada alternativa debe tener máximo 3 dígitos")
    private Integer ladaAlternativo;

    @Min(value = 1000000000L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono alternativo no puede exceder los 10 dígitos")
    private Long telefonoAlternativo;

    // --- Domicilio ---
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    private String calle;

    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
    private String numeroInterior;

    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    private String colonia;

    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    private String municipio;

    private EstadoMexico estado;

    @Min(value = 1000, message = "El código postal debe tener 5 dígitos (o 4 si empieza con 0)")
    @Max(value = 99999, message = "El código postal no puede exceder 5 dígitos")
    private Integer codigoPostal;

    private Pais pais;

    // --- Datos Laborales ---
    @Size(max = 100, message = "La ocupación no puede exceder 100 caracteres")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;
}

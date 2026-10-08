package com.proyecto.servicios.model.onboarding.dto;

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
public class ClienteRegistroDTO {

    // --- Datos Personales ---
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El CURP es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$", message = "Formato de CURP inválido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z\\d]{3}$", message = "Formato de RFC inválido")
    private String rfc;

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Nacionalidad nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    // --- Datos de Contacto ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico es inválido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "La lada móvil es obligatoria")
    @Size(max = 3, message = "La lada no puede ser mayor a 3 caracteres")
    private String ladaMovil;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    private String ladaAlternativo;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    private String telefonoAlternativo;

    // --- Domicilio ---
    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotNull(message = "El estado es obligatorio")
    private EstadoMexico estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotNull(message = "El país es obligatorio")
    private Pais pais;

    // --- Información Laboral ---
    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @Positive(message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    // --- Contraseña de Usuario ---
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial")
    private String password;
}

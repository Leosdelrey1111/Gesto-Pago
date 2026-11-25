package com.proyecto.servicios.model.security;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRapidoRequest {

    // Paso 1: Datos Personales
    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    // Paso 2: Identidad & Fiscal
    @NotBlank(message = "La CURP es obligatoria")
    @Size(min = 18, max = 18, message = "La CURP debe contener exactamente 18 caracteres")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 12, max = 13, message = "El RFC debe contener 12 o 13 caracteres")
    private String rfc;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo electrónico no válido")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefono;

    // Campos KYC de Cliente
    private String sexo;

    private String estadoCivil;

    private String ocupacion;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a 0")
    private java.math.BigDecimal ingresoMensual;

    // Paso 3: Domicilio
    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 2, max = 100, message = "La calle debe tener entre 2 y 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 100, message = "La colonia debe tener entre 2 y 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio")
    @Size(min = 2, max = 100, message = "El municipio debe tener entre 2 y 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotNull(message = "El código postal es obligatorio")
    @Min(value = 1000, message = "El código postal debe ser válido")
    @Max(value = 99999, message = "El código postal no puede exceder 5 dígitos")
    private Integer codigoPostal;

    // Credenciales
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 12, message = "La contraseña debe tener entre 8 y 12 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._\\-*]).*$",
             message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial")
    private String password;
}

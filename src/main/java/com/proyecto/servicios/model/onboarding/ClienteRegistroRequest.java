package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClienteRegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Size(min = 18, max = 18, message = "La CURP debe contener exactamente 18 caracteres")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$", message = "Formato de CURP inválido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 12, max = 13, message = "El RFC debe contener 12 o 13 caracteres")
    @Pattern(regexp = "^([A-ZÑ&]{3,4}) ?(?:- ?)?(\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])) ?(?:- ?)?([A-Z\\d]{2})([A\\d])$", message = "Formato de RFC inválido")
    private String rfc;

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Nacionalidad nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;
    
    @NotNull(message = "El lada móvil es obligatorio")
    @Max(value = 999, message = "La lada debe tener máximo 3 dígitos")
    private Integer ladaMovil;

    @NotNull(message = "El teléfono móvil es obligatorio")
    @Min(value = 1000000000L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono móvil no puede exceder 10 dígitos")
    private Long telefonoMovil;
    
    @Max(value = 999, message = "La lada alternativa debe tener máximo 3 dígitos")
    private Integer ladaAlternativo;

    @Min(value = 1000000000L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono alternativo no puede exceder los 10 dígitos")
    private Long telefonoAlternativo;

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

    @NotNull(message = "El código postal es obligatorio")
    @Min(value = 1000, message = "El código postal debe tener 5 dígitos (o 4 si empieza con 0)")
    @Max(value = 99999, message = "El código postal no puede exceder 5 dígitos")
    private Integer codigoPostal;

    @NotNull(message = "El país es obligatorio")
    private Pais pais;

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe contener mínimo 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "La contraseña debe contener al menos una letra mayúscula, una minúscula, un número y un carácter especial")
    private String password;
}

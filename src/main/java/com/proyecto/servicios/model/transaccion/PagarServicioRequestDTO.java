package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Solicitud de pago de servicio. Todo valor que llegue aqui es no confiable: el monto y el producto
 * se vuelven a validar contra el catalogo en el servicio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagarServicioRequestDTO {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9-]{1,50}$", message = "El numero de cuenta tiene un formato invalido")
    private String numeroCuenta;

    @NotBlank(message = "El idProducto es obligatorio")
    @Pattern(regexp = "^\\d{1,9}$", message = "El idProducto debe ser numerico (maximo 9 digitos)")
    private String idProducto;

    @NotBlank(message = "El idServicio es obligatorio")
    @Pattern(regexp = "^\\d{1,9}$", message = "El idServicio debe ser numerico (maximo 9 digitos)")
    private String idServicio;

    @NotBlank(message = "El telefono o referencia es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{1,30}$", message = "El telefono o referencia solo admite letras y numeros (maximo 30)")
    private String telefonoOreferencia;

    /** Obligatorio para productos de monto abierto; para precio fijo, si se envia debe coincidir con el catalogo. */
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El monto admite maximo 8 enteros y 2 decimales")
    private BigDecimal monto;

    @ToString.Exclude
    @NotBlank(message = "El password o PIN de autorizacion es obligatorio")
    @Size(max = 72, message = "El password o PIN excede la longitud permitida")
    private String passwordOPin;

    /** UUID generado por el cliente. Reenviar la misma clave nunca genera un segundo cargo. */
    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "La clave de idempotencia debe ser un UUID")
    private String claveIdempotencia;
}
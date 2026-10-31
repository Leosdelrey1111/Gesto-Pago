package com.proyecto.servicios.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuracion tipada de las transacciones contra GestoPago (prefijo gestopago.tx).
 * Los secretos llegan por variables de entorno y se validan al arrancar (fail-fast).
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "gestopago.tx")
public class GestoPagoTxProperties {

    @NotBlank
    private String unidad;

    /** Llave del encabezado X-API-Key. Nunca debe imprimirse. */
    @NotBlank
    private String apiKey;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMinimo = new BigDecimal("1.00");

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMaximo = new BigDecimal("10000.00");

    @Min(0)
    private int ventanaDuplicadoSegundos = 60;

    /** Codigos de respuesta del proveedor que significan "en proceso" y no un rechazo definitivo. */
    private Set<String> codigosPendientes = new HashSet<>();

    @Valid
    private Conciliacion conciliacion = new Conciliacion();

    @Getter
    @Setter
    public static class Conciliacion {
        private boolean habilitada = true;
        @Min(1)
        private int esperaMinutos = 5;
        @Min(1)
        private int maxIntentos = 5;
        /** Solo activar si GestoPago garantiza que reenviar el mismo UPC no genera un segundo cargo. */
        private boolean reenvioMismoUpc = false;
    }

    @Override
    public String toString() {
        return "GestoPagoTxProperties[unidad=" + unidad + ", apiKey=***]";
    }
}

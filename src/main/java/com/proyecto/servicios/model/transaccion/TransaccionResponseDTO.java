package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransaccionResponseDTO {

    private Long folioTransaccion;
    private String numeroCuenta;
    private String claveIdempotencia;
    private String upc;
    private String idTxGestoPago;
    private String numAutorizacion;
    private String producto;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal nuevoSaldo;
    private LocalDateTime fecha;
    private EstatusTransaccion estatus;
    private String mensaje;
}
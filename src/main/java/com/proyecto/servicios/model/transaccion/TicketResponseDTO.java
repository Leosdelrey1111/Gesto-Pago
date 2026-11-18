package com.proyecto.servicios.model.transaccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {
    private String titulo;
    private Long folio;
    private String upc;
    private String numeroAutorizacion;
    private String idTransaccionProveedor;
    private String servicio;
    private String producto;
    private String referenciaOTelefono;
    private BigDecimal monto;
    private String numeroCuenta;
    private LocalDateTime fechaOperacion;
    private String estatus;
    private String leyenda;
    private String emisor;
}


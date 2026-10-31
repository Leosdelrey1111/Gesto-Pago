package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;

import java.math.BigDecimal;

/**
 * Resultado de validar una solicitud contra el catalogo: lo unico que se enviara al proveedor.
 * El monto aqui es el autorizado por el servidor, nunca el que el cliente declaro.
 */
public record PagoPlan(CatalogoProducto producto,
                       BigDecimal monto,
                       String telefono,
                       String referencia,
                       boolean montoAbierto) {
}
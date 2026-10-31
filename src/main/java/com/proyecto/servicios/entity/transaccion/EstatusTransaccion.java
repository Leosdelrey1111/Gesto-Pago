package com.proyecto.servicios.entity.transaccion;

/**
 * Ciclo de vida de un pago. El saldo se reserva (descuenta) al crear la transaccion en PENDIENTE.
 *
 * <pre>
 * PENDIENTE --(01)--------------------------> EXITOSA                  (saldo descontado)
 * PENDIENTE --(rechazo definitivo)----------> RECHAZADA                (saldo devuelto)
 * PENDIENTE --(no salio a la red)-----------> FALLIDA                  (saldo devuelto)
 * PENDIENTE --(timeout / 5xx / ilegible)----> PENDIENTE_CONCILIACION   (saldo retenido)
 * PENDIENTE_CONCILIACION --(consulta)-------> EXITOSA | RECHAZADA
 * </pre>
 */
public enum EstatusTransaccion {
    PENDIENTE,
    EXITOSA,
    RECHAZADA,
    FALLIDA,
    PENDIENTE_CONCILIACION;

    public boolean esFinal() {
        return this == EXITOSA || this == RECHAZADA || this == FALLIDA;
    }

    public boolean requiereConciliacion() {
        return this == PENDIENTE || this == PENDIENTE_CONCILIACION;
    }
}

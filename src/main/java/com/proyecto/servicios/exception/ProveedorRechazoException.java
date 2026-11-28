package com.proyecto.servicios.exception;

/**
 * GestoPago respondio de forma concluyente que NO proceso la operacion. El saldo ya fue devuelto.
 * Solo expone el codigo del proveedor; el texto libre del proveedor no se reenvia al cliente.
 */
public class ProveedorRechazoException extends OperacionNoProcesableException {

    private final String codigoProveedor;
    private final Long folioTransaccion;

    public ProveedorRechazoException(String codigoProveedor, Long folioTransaccion) {
        super("El proveedor rechazo la operacion. Codigo: " + codigoProveedor);
        this.codigoProveedor = codigoProveedor;
        this.folioTransaccion = folioTransaccion;
    }

    public ProveedorRechazoException(String codigoProveedor) {
        this(codigoProveedor, null);
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }

    public Long getFolioTransaccion() {
        return folioTransaccion;
    }
}
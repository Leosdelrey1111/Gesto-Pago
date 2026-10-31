package com.proyecto.servicios.exception;

/**
 * GestoPago respondio de forma concluyente que NO proceso la operacion. El saldo ya fue devuelto.
 * Solo expone el codigo del proveedor; el texto libre del proveedor no se reenvia al cliente.
 */
public class ProveedorRechazoException extends OperacionNoProcesableException {

    private final String codigoProveedor;

    public ProveedorRechazoException(String codigoProveedor) {
        super("El proveedor rechazo la operacion. Codigo: " + codigoProveedor);
        this.codigoProveedor = codigoProveedor;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }
}
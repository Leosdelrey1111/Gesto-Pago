package com.proyecto.servicios.exception;

/**
 * El proveedor (GestoPago) no pudo recibir la solicitud: no hay token, no hubo conexion
 * o la peticion fue rechazada antes de ser procesada. Es seguro asumir que NO hubo cargo.
 */
public class ProveedorNoDisponibleException extends RuntimeException {

    public ProveedorNoDisponibleException(String message) {
        super(message);
    }

    public ProveedorNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

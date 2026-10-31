package com.proyecto.servicios.exception;

/**
 * La solicitud es valida en forma pero una regla de negocio impide ejecutarla (HTTP 422).
 */
public class OperacionNoProcesableException extends RuntimeException {
    public OperacionNoProcesableException(String message) {
        super(message);
    }
}
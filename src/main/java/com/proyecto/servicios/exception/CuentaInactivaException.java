package com.proyecto.servicios.exception;

public class CuentaInactivaException extends OperacionNoProcesableException {
    public CuentaInactivaException() {
        super("La cuenta no se encuentra activa para realizar operaciones");
    }
}
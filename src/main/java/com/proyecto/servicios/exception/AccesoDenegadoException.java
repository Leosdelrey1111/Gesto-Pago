package com.proyecto.servicios.exception;

/**
 * El usuario esta autenticado pero no puede operar el recurso (cuenta ajena, cliente bloqueado, etc.).
 * El mensaje es deliberadamente generico para no revelar si el recurso existe.
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
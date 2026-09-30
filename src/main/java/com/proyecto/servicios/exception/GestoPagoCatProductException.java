package com.proyecto.servicios.exception;

public class GestoPagoCatProductException extends RuntimeException {

    public GestoPagoCatProductException(String message) {
        super(message);
    }

    public GestoPagoCatProductException(String message, Throwable cause) {
        super(message, cause);
    }
}
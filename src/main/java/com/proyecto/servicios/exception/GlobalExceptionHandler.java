package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoCatProductException.class)
    public ResponseEntity<GenericResponse> handleGestoPagoCatProduct(GestoPagoCatProductException ex) {
        log.error("Error controlado al obtener catalogo de GestoPago: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(1);
        response.setMensaje(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }
}
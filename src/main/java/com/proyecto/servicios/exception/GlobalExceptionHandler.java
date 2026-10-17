package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.response.GenericResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends com.proyecto.servicios.controller.ValidadorCampos {


    /**
     * Manejo de violaciones de restricciones en parámetros (@RequestParam, @Validated).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            errors.put(fieldName, violation.getMessage());
        });

        log.warn("Violación de restricciones en parámetros: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación en los parámetros de la solicitud", errors);
    }

    /**
     * Manejo cuando falta un parámetro obligatorio en @RequestParam.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, String> errors = Map.of(ex.getParameterName(), "El parámetro es obligatorio");
        log.warn("Parámetro obligatorio faltante: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Parámetro obligatorio faltante", errors);
    }

    /**
     * Manejo de discrepancia de tipos de datos en parámetros (ej. letras en id numérico).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("desconocido");
        Map<String, String> errors = Map.of(ex.getName(), "El valor proporcionado no es válido para el tipo " + requiredType);
        log.warn("Discrepancia de tipo en parámetro {}: {}", ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "Tipo de dato inválido en parámetro", errors);
    }

    /**
     * Manejo de JSON malformado o tipos no convertibles en el cuerpo del request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de solicitud ilegible: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud (JSON) es inválido o contiene tipos de datos incorrectos");
    }

    /**
     * Recurso no encontrado (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<GenericResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Recurso duplicado (409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<GenericResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Validación de regla de negocio (400).
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<GenericResponse<Void>> handleBusinessValidation(BusinessValidationException ex) {
        log.warn("Error de validación de negocio: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Acceso no autorizado o prohibido (401/403).
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<GenericResponse<Void>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Error de servicio externo de GestoPago.
     */
    @ExceptionHandler(GestoPagoCatProductException.class)
    public ResponseEntity<GenericResponse<Void>> handleGestoPagoCatProduct(GestoPagoCatProductException ex) {
        log.error("Error al obtener catálogo de GestoPago: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /**
     * Usuario no encontrado (404).
     */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Void>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Correo duplicado (409).
     */
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Void>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        log.warn("Correo duplicado: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Error en reglas de negocio del sistema (400).
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse<Void>> handleReglaNegocio(ReglaNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Violación de integridad de datos en BD (409) - Evita devolver trazas SQL al cliente.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en la base de datos: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "No se pudo completar la operación debido a un conflicto o restricción de integridad de datos.");
    }

    /**
     * Manejo genérico para excepciones inesperadas no controladas (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Error interno del servidor no controlado: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno en el servidor.");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE CONSTRUCCIÓN DE RESPUESTA
    // =========================================================================

    private ResponseEntity<GenericResponse<Void>> buildResponse(HttpStatus status, String message) {
        return buildResponse(status, message, null);
    }

    private ResponseEntity<GenericResponse<Void>> buildResponse(HttpStatus status, String message, Object errores) {
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .status(status.value())
                .codigo(status.value())
                .mensaje(message)
                .timestamp(LocalDateTime.now())
                .errores(errores)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
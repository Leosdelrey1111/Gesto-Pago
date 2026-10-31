package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.model.response.GenericResponse;
import com.proyecto.servicios.model.transaccion.HistorialTransaccionesRequestDTO;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionHistorialDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;
import com.proyecto.servicios.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sin logica de negocio, sin try-catch y sin @PathVariable/@RequestParam: los errores los traduce
 * {@code GlobalExceptionHandler} y los datos viajan en DTO validados con @Valid.
 */
@RestController
@RequestMapping("/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    /**
     * 200 si el proveedor confirmo; 202 si el resultado esta por conciliarse (el monto queda retenido).
     */
    @PostMapping("/pagar")
    public ResponseEntity<GenericResponse<TransaccionResponseDTO>> pagarServicio(
            @Valid @RequestBody PagarServicioRequestDTO request,
            Authentication authentication) {

        TransaccionResponseDTO resultado = transaccionService.pagarServicio(request, authentication.getName());

        HttpStatus status = resultado.getEstatus() == EstatusTransaccion.EXITOSA ? HttpStatus.OK : HttpStatus.ACCEPTED;
        GenericResponse<TransaccionResponseDTO> respuesta = GenericResponse.<TransaccionResponseDTO>builder()
                .codigo(status.value())
                .status(status.value())
                .mensaje(resultado.getMensaje())
                .data(resultado)
                .build();
        return ResponseEntity.status(status).body(respuesta);
    }

    @PostMapping("/historial")
    public ResponseEntity<GenericResponse<List<TransaccionHistorialDTO>>> consultarHistorial(
            @Valid @RequestBody HistorialTransaccionesRequestDTO request,
            Authentication authentication) {

        List<TransaccionHistorialDTO> historial = transaccionService.obtenerHistorial(request, authentication.getName());

        GenericResponse<List<TransaccionHistorialDTO>> respuesta = GenericResponse.<List<TransaccionHistorialDTO>>builder()
                .codigo(HttpStatus.OK.value())
                .status(HttpStatus.OK.value())
                .mensaje("Historial de transacciones obtenido correctamente")
                .data(historial)
                .build();
        return ResponseEntity.ok(respuesta);
    }
}

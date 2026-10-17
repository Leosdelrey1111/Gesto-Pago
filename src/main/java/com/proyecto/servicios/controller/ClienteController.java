package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import com.proyecto.servicios.model.response.GenericResponse;
import com.proyecto.servicios.model.response.PaginaResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * Registro de nuevo cliente con validaciones automáticas via Bean Validation.
     */
    @PostMapping
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        ClienteResponseDTO cliente = clienteService.registrarCliente(request);
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Cliente registrado exitosamente")
                .data(cliente)
                .status(HttpStatus.CREATED.value())
                .codigo(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Consulta paginada con tamaño y número de página personalizables por RequestParam.
     * Devuelve DTO estructurado en PaginaResponseDTO sin PathVariable ni if anidados.
     */
    @GetMapping
    public ResponseEntity<GenericResponse<PaginaResponseDTO<ClienteResponseDTO>>> obtenerClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno) {

        Page<ClienteResponseDTO> clientes = clienteService.buscarClientes(nombre, apellidoPaterno, apellidoMaterno, page, size);
        GenericResponse<PaginaResponseDTO<ClienteResponseDTO>> response = GenericResponse.<PaginaResponseDTO<ClienteResponseDTO>>builder()
                .mensaje("Consulta exitosa")
                .data(PaginaResponseDTO.desde(clientes))
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta puntual por ID sin usar PathVariable (usando RequestParam).
     */
    @GetMapping("/por-id")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> obtenerPorId(@RequestParam Long id) {
        verificarPropietario(id);
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Consulta exitosa")
                .data(clienteService.buscarCliente(id))
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta puntual combinada por RFC y CURP (findbyrfcandcurp) usando RequestParam.
     */
    @GetMapping("/por-rfc-curp")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> obtenerPorRfcYCurp(
            @RequestParam String rfc,
            @RequestParam String curp) {

        ClienteResponseDTO cliente = clienteService.buscarCliente(rfc, curp);
        verificarPropietario(cliente.getId());
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Consulta exitosa")
                .data(cliente)
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta puntual por CURP usando RequestParam.
     */
    @GetMapping("/por-curp")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> obtenerPorCurp(@RequestParam String curp) {
        ClienteResponseDTO cliente = clienteService.obtenerPorCurp(curp);
        verificarPropietario(cliente.getId());
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Consulta exitosa")
                .data(cliente)
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta puntual por RFC usando RequestParam.
     */
    @GetMapping("/por-rfc")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> obtenerPorRfc(@RequestParam String rfc) {
        ClienteResponseDTO cliente = clienteService.obtenerPorRfc(rfc);
        verificarPropietario(cliente.getId());
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Consulta exitosa")
                .data(cliente)
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Actualización parcial con validaciones Jakarta Bean Validation (@Valid) sin PathVariable.
     */
    @PatchMapping("/actualizar")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> actualizarParcialmente(
            @RequestParam Long id,
            @Valid @RequestBody ClienteActualizaRequest request) {

        verificarPropietario(id);
        ClienteResponseDTO clienteActualizado = clienteService.actualizarParcialmente(id, request);
        GenericResponse<ClienteResponseDTO> response = GenericResponse.<ClienteResponseDTO>builder()
                .mensaje("Cliente actualizado exitosamente")
                .data(clienteActualizado)
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Bloquear cliente sin PathVariable.
     */
    @PatchMapping("/bloquear")
    public ResponseEntity<GenericResponse<Void>> bloquearCliente(@RequestParam Long id) {
        verificarPropietario(id);
        clienteService.bloquear(id);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente bloqueado exitosamente")
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Desbloquear cliente sin PathVariable.
     */
    @PatchMapping("/desbloquear")
    public ResponseEntity<GenericResponse<Void>> desbloquearCliente(@RequestParam Long id) {
        verificarPropietario(id);
        clienteService.desbloquear(id);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente desbloqueado exitosamente")
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Baja lógica de cliente sin PathVariable.
     */
    @DeleteMapping("/baja")
    public ResponseEntity<GenericResponse<Void>> bajaLogica(
            @RequestParam Long id,
            @RequestParam TipoBaja tipoBaja) {

        verificarPropietario(id);
        clienteService.bajaLogica(id, tipoBaja);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente dado de baja exitosamente")
                .status(HttpStatus.OK.value())
                .codigo(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // CONTROL DE AUTORIZACIÓN Y SEGURIDAD IDOR
    // =========================================================================

    private void verificarPropietario(Long id) {
        String correoAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        ClienteResponseDTO cliente = clienteService.buscarCliente(id);
        if (!cliente.getCorreoElectronico().equalsIgnoreCase(correoAutenticado)) {
            throw new UnauthorizedAccessException("No tienes permiso para acceder o modificar este cliente.");
        }
    }
}

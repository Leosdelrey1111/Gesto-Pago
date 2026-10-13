package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.model.response.GenericResponse;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.proyecto.servicios.model.response.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping
    public ResponseEntity<GenericResponse<Cliente>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        Cliente cliente = clienteService.registrarCliente(request);
        GenericResponse<Cliente> response = GenericResponse.<Cliente>builder()
                .mensaje("Cliente registrado exitosamente")
                .data(cliente)
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<GenericResponse<List<Cliente>>> obtenerTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc) {

        List<Cliente> clientes;
        if (curp != null) {
            clientes = List.of(clienteService.obtenerPorCurp(curp));
        } else if (rfc != null) {
            clientes = List.of(clienteService.obtenerPorRfc(rfc));
        } else if (nombre != null) {
            clientes = clienteService.buscarPorNombre(nombre);
        } else if (apellidoPaterno != null) {
            clientes = clienteService.buscarPorApellidoPaterno(apellidoPaterno);
        } else if (apellidoMaterno != null) {
            clientes = clienteService.buscarPorApellidoMaterno(apellidoMaterno);
        } else {
            clientes = clienteService.obtenerTodos();
        }
        
        GenericResponse<List<Cliente>> response = GenericResponse.<List<Cliente>>builder()
                .mensaje("Consulta exitosa")
                .data(clientes)
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse<Cliente>> obtenerPorId(@PathVariable Long id) {
        verificarPropietario(id);
        GenericResponse<Cliente> response = GenericResponse.<Cliente>builder()
                .mensaje("Consulta exitosa")
                .data(clienteService.obtenerPorId(id))
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GenericResponse<Cliente>> actualizarParcialmente(@PathVariable Long id, @RequestBody ClienteActualizaRequest request) {
        verificarPropietario(id);
        GenericResponse<Cliente> response = GenericResponse.<Cliente>builder()
                .mensaje("Cliente actualizado exitosamente")
                .data(clienteService.actualizarParcialmente(id, request))
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/bloquear")
    public ResponseEntity<GenericResponse<Void>> bloquearCliente(@PathVariable Long id) {
        verificarPropietario(id);
        clienteService.bloquear(id);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente bloqueado exitosamente")
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<GenericResponse<Void>> desbloquearCliente(@PathVariable Long id) {
        verificarPropietario(id);
        clienteService.desbloquear(id);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente desbloqueado exitosamente")
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse<Void>> bajaLogica(
            @PathVariable Long id, 
            @RequestParam(required = true) TipoBaja tipoBaja) {
        verificarPropietario(id);
        clienteService.bajaLogica(id, tipoBaja);
        GenericResponse<Void> response = GenericResponse.<Void>builder()
                .mensaje("Cliente dado de baja exitosamente")
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(response);
    }

    private void verificarPropietario(Long id) {
        String correoAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        Cliente cliente = clienteService.obtenerPorId(id);
        if (!cliente.getCorreoElectronico().equals(correoAutenticado)) {
            throw new UnauthorizedAccessException("No tienes permiso para acceder o modificar este cliente.");
        }
    }
}

package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PreAuthorize("hasRole('CLIENTE') and hasAuthority('SCOPE_READ')")
    @GetMapping("/mis-cuentas")
    public ResponseEntity<List<Cuenta>> misCuentas(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (usuario == null || usuario.getCliente() == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(cuentaService.obtenerPorClienteId(usuario.getCliente().getId()));
    }

    @PreAuthorize("(hasRole('CLIENTE') and hasAuthority('SCOPE_READ')) or hasRole('ADMIN')")
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> consultarPorNumeroCuenta(
            @PathVariable String numeroCuenta,
            Authentication authentication) {
        Cuenta cuenta = obtenerCuentaAutorizada(numeroCuenta, authentication);
        return ResponseEntity.ok(cuenta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Cuenta>> buscarCuentas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstatusCuenta estatus) {
        
        if (clienteId != null) {
            return ResponseEntity.ok(cuentaService.obtenerPorClienteId(clienteId));
        } else if (estatus != null) {
            return ResponseEntity.ok(cuentaService.buscarPorEstatus(estatus));
        }
        
        return ResponseEntity.badRequest().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Cuenta> crearCuenta(@RequestParam Long clienteId) {
        return new ResponseEntity<>(cuentaService.crearCuentaParaCliente(clienteId), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> actualizarEstatus(@PathVariable String numeroCuenta, @RequestParam EstatusCuenta estatus) {
        return ResponseEntity.ok(cuentaService.actualizarEstatus(numeroCuenta, estatus));
    }

    @PreAuthorize("hasRole('CLIENTE') and hasAuthority('SCOPE_WRITE')")
    @PatchMapping("/mis-cuentas/estatus")
    public ResponseEntity<?> cambiarEstatusMiCuenta(
            @RequestParam String numeroCuenta,
            @RequestParam EstatusCuenta estatus,
            Authentication authentication) {

        obtenerCuentaAutorizada(numeroCuenta, authentication);
        Cuenta actualizada = cuentaService.actualizarEstatus(numeroCuenta, estatus);
        return ResponseEntity.ok(actualizada);
    }

    private Cuenta obtenerCuentaAutorizada(String numeroCuenta, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccesoDenegadoException("Sesión no autenticada");
        }
        Cuenta cuenta = cuentaService.obtenerPorNumeroCuenta(numeroCuenta);

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (esAdmin) {
            return cuenta;
        }

        Usuario usuario = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (usuario == null || usuario.getCliente() == null
                || cuenta.getCliente() == null
                || !cuenta.getCliente().getId().equals(usuario.getCliente().getId())) {
            throw new AccesoDenegadoException("No tiene autorización sobre esta cuenta");
        }
        return cuenta;
    }
}

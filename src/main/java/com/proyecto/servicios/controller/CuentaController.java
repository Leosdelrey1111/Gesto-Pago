package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

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

    @PostMapping
    public ResponseEntity<Cuenta> crearCuenta(@RequestParam Long clienteId) {
        return new ResponseEntity<>(cuentaService.crearCuentaParaCliente(clienteId), HttpStatus.CREATED);
    }

    @PutMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> actualizarEstatus(@PathVariable String numeroCuenta, @RequestParam EstatusCuenta estatus) {
        return ResponseEntity.ok(cuentaService.actualizarEstatus(numeroCuenta, estatus));
    }
}

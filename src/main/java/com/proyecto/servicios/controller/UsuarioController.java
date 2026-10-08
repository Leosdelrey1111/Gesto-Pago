package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/filtro")
    public ResponseEntity<List<Usuario>> filtrarUsuarios(@RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(usuarioService.obtenerActivos(activo));
    }

    @PutMapping("/agregar")
    public ResponseEntity<Usuario> agregarUsuario(
            @RequestParam Long clienteId,
            @RequestParam String correo,
            @RequestParam String password) {
        return ResponseEntity.ok(usuarioService.crearUsuarioParaCliente(clienteId, correo, password));
    }
}

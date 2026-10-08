package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Usuario;
import java.util.List;

public interface UsuarioService {
    Usuario crearUsuarioParaCliente(Long clienteId, String correo, String rawPassword);
    void inactivarUsuario(Long clienteId);
    List<Usuario> obtenerActivos(Boolean activo);
}

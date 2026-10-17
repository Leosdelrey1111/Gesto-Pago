package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.usuario.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UsuarioService {

    UsuarioResponseDTO crearUsuario(UsuarioCreacionRequest request);

    Page<UsuarioResponseDTO> buscarUsuarios(UsuarioBusquedaRequest request);

    UsuarioResponseDTO actualizarUsuario(UsuarioActualizaRequest request);

    UsuarioResponseDTO cambiarEstado(UsuarioCambioEstadoRequest request);

    void eliminarUsuario(UsuarioEliminaRequest request);

    // Métodos para soporte del flujo de onboarding existente
    Usuario crearUsuarioParaCliente(Long clienteId, String correo, String rawPassword);

    void inactivarUsuario(Long clienteId);

    List<Usuario> obtenerActivos(Boolean activo);
}

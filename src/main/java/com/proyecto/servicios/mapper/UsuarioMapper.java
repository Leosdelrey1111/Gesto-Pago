package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.usuario.UsuarioResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .correo(usuario.getCorreo())
                .activo(usuario.getActivo())
                .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}

package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.usuario.*;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.repository.onboarding.specification.UsuarioSpecification;
import com.proyecto.servicios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioCreacionRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoDuplicadoException("El correo electrónico '" + request.getCorreo() + "' ya se encuentra registrado.");
        }

        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + request.getClienteId()));

            if (usuarioRepository.existsByClienteId(request.getClienteId())) {
                throw new ReglaNegocioException("El cliente con ID " + request.getClienteId() + " ya cuenta con un usuario registrado.");
            }
        }

        Usuario usuario = Usuario.builder()
                .correo(request.getCorreo().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .cliente(cliente)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con ID: {}", guardado.getId());
        return usuarioMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscarUsuarios(UsuarioBusquedaRequest request) {
        int page = 0;
        int size = 10;
        String sortBy = "id";
        Sort.Direction direction = Sort.Direction.ASC;

        if (request != null) {
            if (request.getPagina() != null && request.getPagina() >= 0) {
                page = request.getPagina();
            }
            if (request.getTamanio() != null && request.getTamanio() > 0) {
                size = request.getTamanio();
            }
            if (request.getOrdenarPor() != null && !request.getOrdenarPor().isBlank()) {
                String campo = request.getOrdenarPor().trim();
                if ("id".equalsIgnoreCase(campo) || "correo".equalsIgnoreCase(campo) 
                        || "activo".equalsIgnoreCase(campo) || "fechaCreacion".equalsIgnoreCase(campo)) {
                    sortBy = campo;
                }
            }
            if ("DESC".equalsIgnoreCase(request.getDireccion())) {
                direction = Sort.Direction.DESC;
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Usuario> usuariosPage = usuarioRepository.findAll(UsuarioSpecification.conFiltros(request), pageable);

        return usuariosPage.map(usuarioMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(UsuarioActualizaRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            String nuevoCorreo = request.getCorreo().trim().toLowerCase();
            if (usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, request.getId())) {
                throw new CorreoDuplicadoException("El correo electrónico '" + nuevoCorreo + "' ya está en uso por otro usuario.");
            }
            usuario.setCorreo(nuevoCorreo);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario con ID {} actualizado exitosamente", actualizado.getId());
        return usuarioMapper.toResponseDTO(actualizado);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarEstado(UsuarioCambioEstadoRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        if (usuario.getActivo().equals(request.getActivo())) {
            throw new ReglaNegocioException("El usuario ya se encuentra con el estado: " + (request.getActivo() ? "ACTIVO" : "INACTIVO"));
        }

        usuario.setActivo(request.getActivo());
        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Estado del usuario con ID {} cambiado a: {}", actualizado.getId(), request.getActivo());
        return usuarioMapper.toResponseDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarUsuario(UsuarioEliminaRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario con ID {} dado de baja exitosamente", request.getId());
    }

    // --- Métodos de compatibilidad con ClienteServiceImpl ---

    @Override
    @Transactional
    public Usuario crearUsuarioParaCliente(Long clienteId, String correo, String rawPassword) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException("Ya existe un usuario con el correo: " + correo);
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + clienteId));

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo.trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void inactivarUsuario(Long clienteId) {
        Usuario usuario = usuarioRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado para el cliente con ID: " + clienteId));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> obtenerActivos(Boolean activo) {
        return usuarioRepository.findByActivo(activo);
    }
}

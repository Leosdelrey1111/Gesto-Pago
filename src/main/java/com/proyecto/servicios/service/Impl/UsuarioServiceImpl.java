package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.exception.BusinessValidationException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario crearUsuarioParaCliente(Long clienteId, String correo, String rawPassword) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new BusinessValidationException("Ya existe un usuario con el correo: " + correo);
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + clienteId));

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    @Override
    public void inactivarUsuario(Long clienteId) {
        Usuario usuario = usuarioRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para el cliente: " + clienteId));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public List<Usuario> obtenerActivos(Boolean activo) {
        return usuarioRepository.findByActivo(activo);
    }
}

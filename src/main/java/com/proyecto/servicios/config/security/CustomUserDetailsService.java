package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el correo: " + username));

        if (!usuario.getActivo()) {
            throw new DisabledException("El usuario está inactivo.");
        }

        return new org.springframework.security.core.userdetails.User(usuario.getCorreo(), usuario.getPassword(), new ArrayList<>());
    }
}

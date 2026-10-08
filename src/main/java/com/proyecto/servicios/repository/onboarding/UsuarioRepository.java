package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);
    
    List<Usuario> findByActivo(Boolean activo);

    boolean existsByCorreo(String correo);
}

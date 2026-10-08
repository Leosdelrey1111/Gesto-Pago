package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Long> {
    
    Optional<Domicilio> findByClienteId(Long clienteId);
}

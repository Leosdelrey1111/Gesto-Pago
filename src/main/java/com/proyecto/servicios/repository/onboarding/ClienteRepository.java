package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByCurp(String curp);
    
    Optional<Cliente> findByRfc(String rfc);
    
    Optional<Cliente> findByCorreoElectronico(String correoElectronico);
    
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    
    List<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);
    
    List<Cliente> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno);
    
    List<Cliente> findByActivoTrue();
    
    List<Cliente> findByFechaCreacionBetween(OffsetDateTime start, OffsetDateTime end);
    
    // Validaciones para evitar duplicados
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreoElectronico(String correoElectronico);
}

package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    ClienteResponseDTO registrarCliente(ClienteRegistroRequest request);

    // =========================================================================
    // SOBRECARGA DE MÉTODOS (Method Overloading) PARA CONSULTAS PAGINADAS
    // Permite al cliente modificar el tamaño y número de página de forma flexible
    // =========================================================================

    /**
     * Sobrecarga 1: Obtiene todos los clientes paginados recibiendo un Pageable.
     */
    Page<ClienteResponseDTO> buscarClientes(Pageable pageable);

    /**
     * Sobrecarga 2: Obtiene clientes paginados especificando número de página y tamaño de página.
     */
    Page<ClienteResponseDTO> buscarClientes(int page, int size);

    /**
     * Sobrecarga 3: Búsqueda paginada filtrada por nombre o apellidos recibiendo Pageable.
     */
    Page<ClienteResponseDTO> buscarClientes(String nombre, String apellidoPaterno, String apellidoMaterno, Pageable pageable);

    /**
     * Sobrecarga 4: Búsqueda paginada filtrada especificando número de página y tamaño de página.
     */
    Page<ClienteResponseDTO> buscarClientes(String nombre, String apellidoPaterno, String apellidoMaterno, int page, int size);

    // =========================================================================
    // SOBRECARGA DE MÉTODOS PUNTUALES DE BÚSQUEDA
    // =========================================================================

    /**
     * Sobrecarga 5: Búsqueda puntual por ID.
     */
    ClienteResponseDTO buscarCliente(Long id);

    /**
     * Sobrecarga 6: Búsqueda puntual combinada por RFC y CURP (findbyrfcandcurp).
     */
    ClienteResponseDTO buscarCliente(String rfc, String curp);

    // Métodos semánticos para consultas directas
    ClienteResponseDTO obtenerPorId(Long id);
    ClienteResponseDTO obtenerPorCurp(String curp);
    ClienteResponseDTO obtenerPorRfc(String rfc);
    ClienteResponseDTO obtenerPorRfcYCurp(String rfc, String curp);

    // =========================================================================
    // GESTIÓN Y ACTUALIZACIÓN (Sin sobreescritura de campos no enviados)
    // =========================================================================
    ClienteResponseDTO actualizarParcialmente(Long id, ClienteActualizaRequest request);
    void bajaLogica(Long id, TipoBaja tipoBaja);
    void bloquear(Long id);
    void desbloquear(Long id);
}

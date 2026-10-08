package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    Cliente registrarCliente(ClienteRegistroRequest request);
    List<Cliente> obtenerTodos();
    Cliente obtenerPorId(Long id);
    Cliente obtenerPorCurp(String curp);
    Cliente obtenerPorRfc(String rfc);
    List<Cliente> buscarPorNombre(String nombre);
    List<Cliente> buscarPorApellidoPaterno(String apellidoPaterno);
    List<Cliente> buscarPorApellidoMaterno(String apellidoMaterno);
    Cliente actualizarParcialmente(Long id, ClienteActualizaRequest request);
    void bajaLogica(Long id, TipoBaja tipoBaja);
    void bloquear(Long id);
    void desbloquear(Long id);
}

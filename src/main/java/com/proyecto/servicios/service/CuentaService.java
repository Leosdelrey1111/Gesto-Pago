package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;

import java.util.List;

public interface CuentaService {
    Cuenta crearCuentaParaCliente(Long clienteId);
    Cuenta obtenerPorNumeroCuenta(String numeroCuenta);
    List<Cuenta> obtenerPorClienteId(Long clienteId);
    List<Cuenta> buscarPorEstatus(EstatusCuenta estatus);
    Cuenta actualizarEstatus(String numeroCuenta, EstatusCuenta nuevoEstatus);
}

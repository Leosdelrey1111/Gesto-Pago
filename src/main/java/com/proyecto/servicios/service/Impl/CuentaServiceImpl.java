package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CuentaServiceImpl implements CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public Cuenta crearCuentaParaCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + clienteId));
                
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(new BigDecimal("1000.00")); // Saldo inicial definido por el sistema
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        
        return cuentaRepository.save(cuenta);
    }

    @Override
    public Cuenta obtenerPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con número: " + numeroCuenta));
    }

    @Override
    public List<Cuenta> obtenerPorClienteId(Long clienteId) {
        return cuentaRepository.findByClienteId(clienteId);
    }

    @Override
    public List<Cuenta> buscarPorEstatus(EstatusCuenta estatus) {
        return cuentaRepository.findByEstatus(estatus);
    }

    @Override
    public Cuenta actualizarEstatus(String numeroCuenta, EstatusCuenta nuevoEstatus) {
        Cuenta cuenta = obtenerPorNumeroCuenta(numeroCuenta);
        cuenta.setEstatus(nuevoEstatus);
        return cuentaRepository.save(cuenta);
    }

    private String generarNumeroCuentaUnico() {
        String numCuenta;
        do {
            numCuenta = String.format("%010d", (long) (Math.random() * 10000000000L));
        } while (cuentaRepository.existsByNumeroCuenta(numCuenta));
        return numCuenta;
    }
}

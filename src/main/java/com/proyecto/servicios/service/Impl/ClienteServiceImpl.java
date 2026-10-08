package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;
import com.proyecto.servicios.exception.BusinessValidationException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    @Transactional
    public Cliente registrarCliente(ClienteRegistroRequest request) {
        // Validaciones de negocio
        if (Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new BusinessValidationException("El cliente debe ser mayor de 18 años");
        }
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new DuplicateResourceException("Ya existe un cliente con la CURP proporcionada");
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new DuplicateResourceException("Ya existe un cliente con el RFC proporcionado");
        }
        if (clienteRepository.existsByCorreoElectronico(request.getCorreoElectronico())) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo electrónico proporcionado");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setLadaMovil(request.getLadaMovil());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setLadaAlternativo(request.getLadaAlternativo());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);

        Domicilio domicilio = new Domicilio();
        domicilio.setCalle(request.getCalle());
        domicilio.setNumeroExterior(request.getNumeroExterior());
        domicilio.setNumeroInterior(request.getNumeroInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setCodigoPostal(request.getCodigoPostal());
        domicilio.setPais(request.getPais());
        domicilio.setCliente(cliente);

        cliente.setDomicilio(domicilio);
        
        Cliente savedCliente = clienteRepository.save(cliente);

        // Crear cuenta y usuario automáticamente
        cuentaService.crearCuentaParaCliente(savedCliente.getId());
        usuarioService.crearUsuarioParaCliente(savedCliente.getId(), savedCliente.getCorreoElectronico(), request.getPassword());

        return savedCliente;
    }

    @Override
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    @Override
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @Override
    public Cliente obtenerPorCurp(String curp) {
        return clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con CURP: " + curp));
    }

    @Override
    public Cliente obtenerPorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con RFC: " + rfc));
    }

    @Override
    public List<Cliente> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public List<Cliente> buscarPorApellidoPaterno(String apellidoPaterno) {
        return clienteRepository.findByApellidoPaternoContainingIgnoreCase(apellidoPaterno);
    }

    @Override
    public List<Cliente> buscarPorApellidoMaterno(String apellidoMaterno) {
        return clienteRepository.findByApellidoMaternoContainingIgnoreCase(apellidoMaterno);
    }

    @Override
    @Transactional
    public Cliente actualizarParcialmente(Long id, ClienteActualizaRequest request) {
        Cliente cliente = obtenerPorId(id);

        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getFechaNacimiento() != null) {
            if (Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
                throw new BusinessValidationException("El cliente debe ser mayor de 18 años");
            }
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getSexo() != null) cliente.setSexo(request.getSexo());
        if (request.getNacionalidad() != null) cliente.setNacionalidad(request.getNacionalidad());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());
        if (request.getLadaMovil() != null) cliente.setLadaMovil(request.getLadaMovil());
        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getLadaAlternativo() != null) cliente.setLadaAlternativo(request.getLadaAlternativo());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());

        // Actualizar Domicilio
        Domicilio domicilio = cliente.getDomicilio();
        if (domicilio != null) {
            if (request.getCalle() != null) domicilio.setCalle(request.getCalle());
            if (request.getNumeroExterior() != null) domicilio.setNumeroExterior(request.getNumeroExterior());
            if (request.getNumeroInterior() != null) domicilio.setNumeroInterior(request.getNumeroInterior());
            if (request.getColonia() != null) domicilio.setColonia(request.getColonia());
            if (request.getMunicipio() != null) domicilio.setMunicipio(request.getMunicipio());
            if (request.getEstado() != null) domicilio.setEstado(request.getEstado());
            if (request.getCodigoPostal() != null) domicilio.setCodigoPostal(request.getCodigoPostal());
            if (request.getPais() != null) domicilio.setPais(request.getPais());
        }
        
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void bajaLogica(Long id, TipoBaja tipoBaja) {
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(false);
        cliente.setTipoBaja(tipoBaja);
        clienteRepository.save(cliente);
        usuarioService.inactivarUsuario(id);
    }

    @Override
    @Transactional
    public void bloquear(Long id) {
        Cliente cliente = obtenerPorId(id);
        cliente.setBloqueado(true);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void desbloquear(Long id) {
        Cliente cliente = obtenerPorId(id);
        cliente.setBloqueado(false);
        clienteRepository.save(cliente);
    }
}

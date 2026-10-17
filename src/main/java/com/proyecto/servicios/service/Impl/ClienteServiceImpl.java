package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import com.proyecto.servicios.exception.BusinessValidationException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequest;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaService cuentaService;
    private final UsuarioService usuarioService;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public ClienteResponseDTO registrarCliente(ClienteRegistroRequest request) {
        // Validaciones funcionales sin cascades de if
        Optional.of(request.getFechaNacimiento())
                .filter(fecha -> Period.between(fecha, LocalDate.now()).getYears() >= 18)
                .orElseThrow(() -> new BusinessValidationException("El cliente debe ser mayor de 18 años"));

        validarUnicidad(!clienteRepository.existsByCurp(request.getCurp()), "Ya existe un cliente con la CURP proporcionada");
        validarUnicidad(!clienteRepository.existsByRfc(request.getRfc()), "Ya existe un cliente con el RFC proporcionado");
        validarUnicidad(!clienteRepository.existsByCorreoElectronico(request.getCorreoElectronico()), "Ya existe un cliente con el correo electrónico proporcionado");

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

        return clienteMapper.toResponseDTO(savedCliente);
    }

    // =========================================================================
    // SOBRECARGA DE MÉTODOS DE BÚSQUEDA Y PAGINACIÓN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(Pageable pageable) {
        return clienteMapper.toResponseDTOPage(clienteRepository.findAll(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return buscarClientes(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(String nombre, String apellidoPaterno, String apellidoMaterno, Pageable pageable) {
        Specification<Cliente> spec = Specification.where(
                Optional.ofNullable(nombre)
                        .filter(n -> !n.isBlank())
                        .map(n -> (Specification<Cliente>) (root, query, cb) ->
                                cb.like(cb.lower(root.get("nombre")), "%" + n.trim().toLowerCase() + "%"))
                        .orElse(null)
        ).and(
                Optional.ofNullable(apellidoPaterno)
                        .filter(ap -> !ap.isBlank())
                        .map(ap -> (Specification<Cliente>) (root, query, cb) ->
                                cb.like(cb.lower(root.get("apellidoPaterno")), "%" + ap.trim().toLowerCase() + "%"))
                        .orElse(null)
        ).and(
                Optional.ofNullable(apellidoMaterno)
                        .filter(am -> !am.isBlank())
                        .map(am -> (Specification<Cliente>) (root, query, cb) ->
                                cb.like(cb.lower(root.get("apellidoMaterno")), "%" + am.trim().toLowerCase() + "%"))
                        .orElse(null)
        );

        return clienteMapper.toResponseDTOPage(clienteRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(String nombre, String apellidoPaterno, String apellidoMaterno, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return buscarClientes(nombre, apellidoPaterno, apellidoMaterno, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarCliente(Long id) {
        return obtenerPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarCliente(String rfc, String curp) {
        return obtenerPorRfcYCurp(rfc, curp);
    }

    // =========================================================================
    // CONSULTAS ESPECÍFICAS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorId(Long id) {
        return clienteMapper.toResponseDTO(findClienteEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorCurp(String curp) {
        return clienteRepository.findByCurp(curp)
                .map(clienteMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con CURP: " + curp));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc)
                .map(clienteMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con RFC: " + rfc));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorRfcYCurp(String rfc, String curp) {
        return clienteRepository.findByRfcAndCurp(rfc, curp)
                .map(clienteMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con RFC: " + rfc + " y CURP: " + curp));
    }

    // =========================================================================
    // ACTUALIZACIÓN PARCIAL (No sobreescritura de campos con null, sin if excesivos)
    // =========================================================================

    @Override
    @Transactional
    public ClienteResponseDTO actualizarParcialmente(Long id, ClienteActualizaRequest request) {
        Cliente cliente = findClienteEntityById(id);

        // Actualización declarativa y funcional evitando 20+ sentencias if imperativas
        Optional.ofNullable(request.getNombre()).ifPresent(cliente::setNombre);
        Optional.ofNullable(request.getSegundoNombre()).ifPresent(cliente::setSegundoNombre);
        Optional.ofNullable(request.getApellidoPaterno()).ifPresent(cliente::setApellidoPaterno);
        Optional.ofNullable(request.getApellidoMaterno()).ifPresent(cliente::setApellidoMaterno);
        Optional.ofNullable(request.getFechaNacimiento()).ifPresent(fecha -> {
            Optional.of(fecha)
                    .filter(f -> Period.between(f, LocalDate.now()).getYears() >= 18)
                    .orElseThrow(() -> new BusinessValidationException("El cliente debe ser mayor de 18 años"));
            cliente.setFechaNacimiento(fecha);
        });
        Optional.ofNullable(request.getSexo()).ifPresent(cliente::setSexo);
        Optional.ofNullable(request.getNacionalidad()).ifPresent(cliente::setNacionalidad);
        Optional.ofNullable(request.getEstadoCivil()).ifPresent(cliente::setEstadoCivil);
        Optional.ofNullable(request.getCorreoElectronico()).ifPresent(cliente::setCorreoElectronico);
        Optional.ofNullable(request.getLadaMovil()).ifPresent(cliente::setLadaMovil);
        Optional.ofNullable(request.getTelefonoMovil()).ifPresent(cliente::setTelefonoMovil);
        Optional.ofNullable(request.getLadaAlternativo()).ifPresent(cliente::setLadaAlternativo);
        Optional.ofNullable(request.getTelefonoAlternativo()).ifPresent(cliente::setTelefonoAlternativo);
        Optional.ofNullable(request.getOcupacion()).ifPresent(cliente::setOcupacion);
        Optional.ofNullable(request.getEmpresa()).ifPresent(cliente::setEmpresa);
        Optional.ofNullable(request.getIngresoMensual()).ifPresent(cliente::setIngresoMensual);

        // Actualización de Domicilio sin sobreescribir valores no enviados
        Optional.ofNullable(cliente.getDomicilio()).ifPresent(domicilio -> {
            Optional.ofNullable(request.getCalle()).ifPresent(domicilio::setCalle);
            Optional.ofNullable(request.getNumeroExterior()).ifPresent(domicilio::setNumeroExterior);
            Optional.ofNullable(request.getNumeroInterior()).ifPresent(domicilio::setNumeroInterior);
            Optional.ofNullable(request.getColonia()).ifPresent(domicilio::setColonia);
            Optional.ofNullable(request.getMunicipio()).ifPresent(domicilio::setMunicipio);
            Optional.ofNullable(request.getEstado()).ifPresent(domicilio::setEstado);
            Optional.ofNullable(request.getCodigoPostal()).ifPresent(domicilio::setCodigoPostal);
            Optional.ofNullable(request.getPais()).ifPresent(domicilio::setPais);
        });

        Cliente clienteGuardado = clienteRepository.save(cliente);
        return clienteMapper.toResponseDTO(clienteGuardado);
    }

    @Override
    @Transactional
    public void bajaLogica(Long id, TipoBaja tipoBaja) {
        Cliente cliente = findClienteEntityById(id);
        cliente.setActivo(false);
        cliente.setTipoBaja(tipoBaja);
        clienteRepository.save(cliente);
        usuarioService.inactivarUsuario(id);
    }

    @Override
    @Transactional
    public void bloquear(Long id) {
        Cliente cliente = findClienteEntityById(id);
        cliente.setBloqueado(true);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void desbloquear(Long id) {
        Cliente cliente = findClienteEntityById(id);
        cliente.setBloqueado(false);
        clienteRepository.save(cliente);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private Cliente findClienteEntityById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    private void validarUnicidad(boolean esValido, String mensajeError) {
        if (!esValido) {
            throw new DuplicateResourceException(mensajeError);
        }
    }
}

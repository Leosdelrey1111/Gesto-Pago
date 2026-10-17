package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas unitarias de ClienteServiceImpl")
class ClienteServiceImplTest {

    private ClienteRepository clienteRepository;
    private CuentaService cuentaService;
    private UsuarioService usuarioService;
    private ClienteMapper clienteMapper;
    private ClienteServiceImpl clienteService;

    private Cliente clienteMock;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        cuentaService = mock(CuentaService.class);
        usuarioService = mock(UsuarioService.class);
        clienteMapper = new ClienteMapper();
        clienteService = new ClienteServiceImpl(clienteRepository, cuentaService, usuarioService, clienteMapper);

        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .curp("PERL800101HDFRRN01")
                .rfc("PERL800101ABC")
                .correoElectronico("juan.perez@example.com")
                .ingresoMensual(new BigDecimal("25000.00"))
                .activo(true)
                .bloqueado(false)
                .domicilio(Domicilio.builder()
                        .id(1L)
                        .calle("Av. Reforma")
                        .numeroExterior("123")
                        .colonia("Centro")
                        .municipio("Cuauhtémoc")
                        .codigoPostal(6000)
                        .build())
                .build();
    }

    @Test
    @DisplayName("buscarClientes con paginación devuelve Page de ClienteResponseDTO")
    void buscarClientes_paginado_exito() {
        Page<Cliente> page = new PageImpl<>(List.of(clienteMock));
        when(clienteRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ClienteResponseDTO> resultado = clienteService.buscarClientes(0, 10);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals("Juan", resultado.getContent().get(0).getNombre());
        assertEquals("Juan Pérez López", resultado.getContent().get(0).getNombreCompleto());
        verify(clienteRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Sobrecarga buscarCliente por ID")
    void buscarCliente_porId_exito() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));

        ClienteResponseDTO resultado = clienteService.buscarCliente(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("PERL800101ABC", resultado.getRfc());
    }

    @Test
    @DisplayName("Sobrecarga buscarCliente por RFC y CURP (findbyrfcandcurp)")
    void buscarCliente_porRfcYCurp_exito() {
        when(clienteRepository.findByRfcAndCurp("PERL800101ABC", "PERL800101HDFRRN01"))
                .thenReturn(Optional.of(clienteMock));

        ClienteResponseDTO resultado = clienteService.buscarCliente("PERL800101ABC", "PERL800101HDFRRN01");

        assertNotNull(resultado);
        assertEquals("PERL800101ABC", resultado.getRfc());
        assertEquals("PERL800101HDFRRN01", resultado.getCurp());
        verify(clienteRepository).findByRfcAndCurp("PERL800101ABC", "PERL800101HDFRRN01");
    }

    @Test
    @DisplayName("buscarCliente por RFC y CURP no encontrado lanza ResourceNotFoundException")
    void buscarCliente_porRfcYCurp_noEncontrado_lanzaExcepcion() {
        when(clienteRepository.findByRfcAndCurp("NOEXISTE", "NOEXISTE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                clienteService.buscarCliente("NOEXISTE", "NOEXISTE")
        );
    }

    @Test
    @DisplayName("actualizarParcialmente no sobreescribe campos no enviados (no sobreescritura)")
    void actualizarParcialmente_noSobreescribeConNulos() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Solo enviamos ocupacion, los demás campos deben permanecer intactos
        ClienteActualizaRequest request = ClienteActualizaRequest.builder()
                .ocupacion("Arquitecto de Software")
                .build();

        ClienteResponseDTO resultado = clienteService.actualizarParcialmente(1L, request);

        assertNotNull(resultado);
        assertEquals("Arquitecto de Software", resultado.getOcupacion());
        // Verificamos que los campos anteriores NO fueron sobreescritos
        assertEquals("Juan", resultado.getNombre());
        assertEquals("Pérez", resultado.getApellidoPaterno());
        assertEquals("PERL800101ABC", resultado.getRfc());
        assertEquals("juan.perez@example.com", resultado.getCorreoElectronico());
        assertNotNull(resultado.getDomicilio());
        assertEquals("Av. Reforma", resultado.getDomicilio().getCalle());
    }
}

package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.usuario.*;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas unitarias de UsuarioServiceImpl")
class UsuarioServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private ClienteRepository clienteRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioMapper usuarioMapper;
    private UsuarioServiceImpl usuarioService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        usuarioMapper = new UsuarioMapper();
        usuarioService = new UsuarioServiceImpl(usuarioRepository, clienteRepository, passwordEncoder, usuarioMapper);

        usuarioMock = Usuario.builder()
                .id(1L)
                .correo("admin@empresa.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .fechaCreacion(OffsetDateTime.now())
                .fechaActualizacion(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("crearUsuario - Éxito al registrar usuario y encriptar contraseña")
    void crearUsuario_exitoso() {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("nuevo@empresa.com")
                .password("Password123!")
                .activo(true)
                .build();

        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UsuarioResponseDTO resultado = usuarioService.crearUsuario(request);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getId());
        assertEquals("nuevo@empresa.com", resultado.getCorreo());
        assertTrue(resultado.getActivo());
        verify(passwordEncoder).encode("Password123!");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("crearUsuario - Falla cuando el correo ya existe lanzando CorreoDuplicadoException")
    void crearUsuario_correoDuplicado_lanzaExcepcion() {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("existente@empresa.com")
                .password("Password123!")
                .build();

        when(usuarioRepository.existsByCorreo("existente@empresa.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.crearUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario - Falla cuando el clienteId no existe")
    void crearUsuario_clienteNoExiste_lanzaExcepcion() {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("test@empresa.com")
                .password("Password123!")
                .clienteId(999L)
                .build();

        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario - Falla cuando el cliente ya tiene usuario asignado")
    void crearUsuario_clienteYaTieneUsuario_lanzaExcepcion() {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("test@empresa.com")
                .password("Password123!")
                .clienteId(5L)
                .build();

        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(Cliente.builder().id(5L).build()));
        when(usuarioRepository.existsByClienteId(5L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("buscarUsuarios - Búsqueda dinámica con Specification y paginación")
    void buscarUsuarios_conFiltros() {
        UsuarioBusquedaRequest request = UsuarioBusquedaRequest.builder()
                .correo("admin")
                .activo(true)
                .pagina(0)
                .tamanio(10)
                .build();

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuarioMock)));

        Page<UsuarioResponseDTO> resultado = usuarioService.buscarUsuarios(request);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("admin@empresa.com", resultado.getContent().get(0).getCorreo());
    }

    @Test
    @DisplayName("actualizarUsuario - Éxito al actualizar correo y estado")
    void actualizarUsuario_exitoso() {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(1L)
                .correo("nuevo.admin@empresa.com")
                .activo(false)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.existsByCorreoAndIdNot("nuevo.admin@empresa.com", 1L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDTO resultado = usuarioService.actualizarUsuario(request);

        assertNotNull(resultado);
        assertEquals("nuevo.admin@empresa.com", resultado.getCorreo());
        assertFalse(resultado.getActivo());
    }

    @Test
    @DisplayName("actualizarUsuario - Usuario no encontrado lanza UsuarioNoEncontradoException")
    void actualizarUsuario_noEncontrado_lanzaExcepcion() {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(99L)
                .build();

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.actualizarUsuario(request));
    }

    @Test
    @DisplayName("actualizarUsuario - Correo en uso por otro usuario lanza CorreoDuplicadoException")
    void actualizarUsuario_correoEnUso_lanzaExcepcion() {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(1L)
                .correo("otro@empresa.com")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.existsByCorreoAndIdNot("otro@empresa.com", 1L)).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.actualizarUsuario(request));
    }

    @Test
    @DisplayName("cambiarEstado - Éxito al cambiar estado")
    void cambiarEstado_exitoso() {
        UsuarioCambioEstadoRequest request = UsuarioCambioEstadoRequest.builder()
                .id(1L)
                .activo(false)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDTO resultado = usuarioService.cambiarEstado(request);

        assertNotNull(resultado);
        assertFalse(resultado.getActivo());
    }

    @Test
    @DisplayName("cambiarEstado - Mismo estado lanza ReglaNegocioException")
    void cambiarEstado_mismoEstado_lanzaExcepcion() {
        UsuarioCambioEstadoRequest request = UsuarioCambioEstadoRequest.builder()
                .id(1L)
                .activo(true)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.cambiarEstado(request));
    }

    @Test
    @DisplayName("eliminarUsuario - Realiza baja lógica cambiando activo a false")
    void eliminarUsuario_exitoso() {
        UsuarioEliminaRequest request = UsuarioEliminaRequest.builder()
                .id(1L)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        usuarioService.eliminarUsuario(request);

        assertFalse(usuarioMock.getActivo());
        verify(usuarioRepository).save(usuarioMock);
    }
}

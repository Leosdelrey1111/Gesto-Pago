package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.usuario.*;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de UsuarioController con GlobalExceptionHandler")
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /usuarios - Crear usuario exitosamente (201 Created)")
    void crearUsuario_exitoso_retorna201() throws Exception {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("nuevo.usuario@empresa.com")
                .password("ClaveSegura123!")
                .activo(true)
                .build();

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id(10L)
                .correo("nuevo.usuario@empresa.com")
                .activo(true)
                .fechaCreacion(OffsetDateTime.now())
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCreacionRequest.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.mensaje").value("Usuario creado exitosamente"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.correo").value("nuevo.usuario@empresa.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /usuarios - Validación de Bean Validation falla cuando correo es inválido o password débil (400 Bad Request)")
    void crearUsuario_validacionFalla_retorna400() throws Exception {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("correo-invalido")
                .password("debil")
                .build();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("Datos no validos")))
                .andExpect(jsonPath("$.errores.correo").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    @DisplayName("POST /usuarios/buscar - Consulta dinámica combinando filtros en un único endpoint sin @RequestParam ni @PathVariable (200 OK)")
    void buscarUsuarios_conFiltrosDinamicos_retorna200() throws Exception {
        UsuarioBusquedaRequest request = UsuarioBusquedaRequest.builder()
                .correo("usuario")
                .activo(true)
                .pagina(0)
                .tamanio(5)
                .build();

        UsuarioResponseDTO dto = UsuarioResponseDTO.builder()
                .id(1L)
                .correo("usuario@empresa.com")
                .activo(true)
                .build();

        when(usuarioService.buscarUsuarios(any(UsuarioBusquedaRequest.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Búsqueda de usuarios realizada exitosamente"))
                .andExpect(jsonPath("$.data.contenido").isArray())
                .andExpect(jsonPath("$.data.contenido[0].id").value(1))
                .andExpect(jsonPath("$.data.contenido[0].correo").value("usuario@empresa.com"));
    }

    @Test
    @DisplayName("PUT /usuarios - Actualizar usuario exitosamente sin @PathVariable (200 OK)")
    void actualizarUsuario_exitoso_retorna200() throws Exception {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(1L)
                .correo("actualizado@empresa.com")
                .activo(true)
                .build();

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .correo("actualizado@empresa.com")
                .activo(true)
                .build();

        when(usuarioService.actualizarUsuario(any(UsuarioActualizaRequest.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.correo").value("actualizado@empresa.com"));
    }

    @Test
    @DisplayName("PATCH /usuarios/estado - Cambio de estado de usuario sin @PathVariable (200 OK)")
    void cambiarEstado_exitoso_retorna200() throws Exception {
        UsuarioCambioEstadoRequest request = UsuarioCambioEstadoRequest.builder()
                .id(1L)
                .activo(false)
                .build();

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .correo("usuario@empresa.com")
                .activo(false)
                .build();

        when(usuarioService.cambiarEstado(any(UsuarioCambioEstadoRequest.class))).thenReturn(responseDTO);

        mockMvc.perform(patch("/usuarios/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.activo").value(false));
    }

    @Test
    @DisplayName("DELETE /usuarios - Baja lógica o eliminación con Request DTO (200 OK)")
    void eliminarUsuario_exitoso_retorna200() throws Exception {
        UsuarioEliminaRequest request = UsuarioEliminaRequest.builder()
                .id(1L)
                .build();

        doNothing().when(usuarioService).eliminarUsuario(any(UsuarioEliminaRequest.class));

        mockMvc.perform(delete("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Usuario eliminado exitosamente"));
    }

    @Test
    @DisplayName("Manejo de excepción de negocio: UsuarioNoEncontradoException retorna 404 NOT_FOUND")
    void usuarioNoEncontrado_retorna404() throws Exception {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(999L)
                .correo("test@correo.com")
                .build();

        when(usuarioService.actualizarUsuario(any(UsuarioActualizaRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado con ID: 999"));

        mockMvc.perform(put("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con ID: 999"));
    }

    @Test
    @DisplayName("Manejo de excepción de negocio: CorreoDuplicadoException retorna 409 CONFLICT")
    void correoDuplicado_retorna409() throws Exception {
        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .correo("duplicado@empresa.com")
                .password("ClaveSegura123!")
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCreacionRequest.class)))
                .thenThrow(new CorreoDuplicadoException("El correo electrónico 'duplicado@empresa.com' ya se encuentra registrado."));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico 'duplicado@empresa.com' ya se encuentra registrado."));
    }

    @Test
    @DisplayName("Manejo de excepción de negocio: ReglaNegocioException retorna 400 BAD_REQUEST")
    void reglaNegocio_retorna400() throws Exception {
        UsuarioCambioEstadoRequest request = UsuarioCambioEstadoRequest.builder()
                .id(1L)
                .activo(true)
                .build();

        when(usuarioService.cambiarEstado(any(UsuarioCambioEstadoRequest.class)))
                .thenThrow(new ReglaNegocioException("El usuario ya se encuentra con el estado: ACTIVO"));

        mockMvc.perform(patch("/usuarios/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("El usuario ya se encuentra con el estado: ACTIVO"));
    }
}

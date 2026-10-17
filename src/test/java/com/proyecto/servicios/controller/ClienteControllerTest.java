package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.ClienteActualizaRequest;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de ClienteController con GlobalExceptionHandler")
class ClienteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan.perez@example.com", "password", Collections.emptyList())
        );
    }

    @Test
    @DisplayName("GET /clientes con parametros paginados modificables page y size")
    void obtenerClientes_paginado_retorna200() throws Exception {
        ClienteResponseDTO dto = ClienteResponseDTO.builder()
                .id(1L)
                .nombre("Juan")
                .correoElectronico("juan.perez@example.com")
                .build();

        when(clienteService.buscarClientes(isNull(), isNull(), isNull(), eq(1), eq(5)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(1, 5), 1));

        mockMvc.perform(get("/clientes")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data.pagina").value(1))
                .andExpect(jsonPath("$.data.tamanio").value(5))
                .andExpect(jsonPath("$.data.contenido[0].nombre").value("Juan"));
    }

    @Test
    @DisplayName("GET /clientes/por-rfc-curp busca por rfc y curp sin PathVariable")
    void obtenerPorRfcYCurp_retorna200() throws Exception {
        ClienteResponseDTO dto = ClienteResponseDTO.builder()
                .id(1L)
                .nombre("Juan")
                .curp("PERL800101HDFRRN01")
                .rfc("PERL800101ABC")
                .correoElectronico("juan.perez@example.com")
                .build();

        when(clienteService.buscarCliente("PERL800101ABC", "PERL800101HDFRRN01")).thenReturn(dto);
        when(clienteService.buscarCliente(1L)).thenReturn(dto);

        mockMvc.perform(get("/clientes/por-rfc-curp")
                        .param("rfc", "PERL800101ABC")
                        .param("curp", "PERL800101HDFRRN01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rfc").value("PERL800101ABC"))
                .andExpect(jsonPath("$.data.curp").value("PERL800101HDFRRN01"));
    }

    @Test
    @DisplayName("PATCH /clientes/actualizar con regex invalido retorna 400 controlado por GlobalExceptionHandler")
    void actualizarParcialmente_regexInvalido_retorna400DesdeExceptionHandler() throws Exception {
        // Nombre con números no cumple el regex "^[a-zA-ZñÑáéíóúÁÉÍÓÚ\\s]+$"
        ClienteActualizaRequest request = ClienteActualizaRequest.builder()
                .nombre("Juan123Invalido")
                .build();

        mockMvc.perform(patch("/clientes/actualizar")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("Datos no validos")))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre solo debe contener letras y espacios"));
    }
}

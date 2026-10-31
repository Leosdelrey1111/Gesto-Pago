package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.MontoNoPermitidoException;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.ProveedorRechazoException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;
import com.proyecto.servicios.service.TransaccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionController + GlobalExceptionHandler: codigos HTTP y datos expuestos")
class TransaccionControllerTest {

    private static final String CLAVE = "6f1c2b3a-1111-4222-8333-444455556666";
    private static final String CORREO = "ana@correo.com";

    @Mock
    private TransaccionService transaccionService;

    @InjectMocks
    private TransaccionController controller;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private PagarServicioRequestDTO valida() {
        return PagarServicioRequestDTO.builder().numeroCuenta("0000000001").idProducto("582").idServicio("133")
                .telefonoOreferencia("5512345678").passwordOPin("PIN-SECRETO-1234").claveIdempotencia(CLAVE).build();
    }

    private MockHttpServletRequestBuilder pagar(Object body) throws Exception {
        return post("/transacciones/pagar")
                .principal(new UsernamePasswordAuthenticationToken(CORREO, null, Collections.emptyList()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String s ? s : objectMapper.writeValueAsString(body));
    }

    private TransaccionResponseDTO respuesta(EstatusTransaccion estatus) {
        return TransaccionResponseDTO.builder().folioTransaccion(1L).estatus(estatus).monto(new BigDecimal("100.00"))
                .mensaje("mensaje").build();
    }

    // ---------- exito ----------

    @Test
    @DisplayName("EXITOSA -> 200 y la identidad enviada al servicio sale del token, no del cuerpo")
    void exito200() throws Exception {
        when(transaccionService.pagarServicio(any(), eq(CORREO))).thenReturn(respuesta(EstatusTransaccion.EXITOSA));
        mockMvc.perform(pagar(valida()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.estatus").value("EXITOSA"));
    }

    @Test
    @DisplayName("PENDIENTE_CONCILIACION -> 202 Accepted")
    void pendiente202() throws Exception {
        when(transaccionService.pagarServicio(any(), eq(CORREO))).thenReturn(respuesta(EstatusTransaccion.PENDIENTE_CONCILIACION));
        mockMvc.perform(pagar(valida()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.estatus").value("PENDIENTE_CONCILIACION"));
    }

    // ---------- validacion de entrada (400) ----------

    @Test
    @DisplayName("Campos obligatorios ausentes -> 400 con detalle por campo y sin llegar al servicio")
    void camposObligatorios() throws Exception {
        mockMvc.perform(pagar("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numeroCuenta").exists())
                .andExpect(jsonPath("$.errores.idProducto").exists())
                .andExpect(jsonPath("$.errores.idServicio").exists())
                .andExpect(jsonPath("$.errores.telefonoOreferencia").exists())
                .andExpect(jsonPath("$.errores.passwordOPin").exists())
                .andExpect(jsonPath("$.errores.claveIdempotencia").exists());
        verifyNoInteractions(transaccionService);
    }

    @Test
    @DisplayName("Formatos invalidos (inyeccion, ids no numericos, monto negativo/con 3 decimales, clave no UUID) -> 400")
    void formatosInvalidos() throws Exception {
        PagarServicioRequestDTO r = valida();
        r.setNumeroCuenta("1'; DROP TABLE cuentas;--");
        r.setIdProducto("58a");
        r.setIdServicio("99999999999");
        r.setTelefonoOreferencia("55 1234");
        r.setMonto(new BigDecimal("-5"));
        r.setClaveIdempotencia("no-es-uuid");
        mockMvc.perform(pagar(r))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numeroCuenta").exists())
                .andExpect(jsonPath("$.errores.idProducto").exists())
                .andExpect(jsonPath("$.errores.idServicio").exists())
                .andExpect(jsonPath("$.errores.telefonoOreferencia").exists())
                .andExpect(jsonPath("$.errores.monto").exists())
                .andExpect(jsonPath("$.errores.claveIdempotencia").exists());

        r = valida();
        r.setMonto(new BigDecimal("10.123"));
        mockMvc.perform(pagar(r)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.monto").exists());
        verifyNoInteractions(transaccionService);
    }

    @Test
    @DisplayName("La respuesta de validacion nunca refleja el PIN enviado")
    void validacionNoReflejaPin() throws Exception {
        PagarServicioRequestDTO r = valida();
        r.setClaveIdempotencia("invalida");
        MvcResult res = mockMvc.perform(pagar(r)).andExpect(status().isBadRequest()).andReturn();
        assertFalse(res.getResponse().getContentAsString().contains("PIN-SECRETO-1234"));
    }

    @Test
    @DisplayName("JSON malformado -> 400 generico sin eco del cuerpo")
    void jsonMalformado() throws Exception {
        MvcResult res = mockMvc.perform(pagar("{\"passwordOPin\":\"PIN-SECRETO-1234\", \"monto\": }"))
                .andExpect(status().isBadRequest()).andReturn();
        assertFalse(res.getResponse().getContentAsString().contains("PIN-SECRETO-1234"));
    }

    @Test
    @DisplayName("PagarServicioRequestDTO.toString() no imprime el PIN (no se filtra a logs)")
    void toStringSinPin() {
        assertFalse(valida().toString().contains("PIN-SECRETO-1234"));
    }

    // ---------- reglas de negocio y autorizacion ----------

    @Test
    @DisplayName("PIN incorrecto -> 401")
    void pinIncorrecto() throws Exception {
        when(transaccionService.pagarServicio(any(), any())).thenThrow(new UnauthorizedAccessException("El password o PIN de autorizacion es incorrecto"));
        mockMvc.perform(pagar(valida())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cuenta ajena -> 403")
    void cuentaAjena() throws Exception {
        when(transaccionService.pagarServicio(any(), any())).thenThrow(new AccesoDenegadoException("No tiene permiso para operar esta cuenta"));
        mockMvc.perform(pagar(valida())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Producto inexistente -> 404")
    void productoInexistente() throws Exception {
        when(transaccionService.pagarServicio(any(), any())).thenThrow(new ResourceNotFoundException("no existe"));
        mockMvc.perform(pagar(valida())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Monto manipulado, saldo insuficiente y cuenta inactiva -> 422")
    void reglasDeNegocio422() throws Exception {
        for (RuntimeException e : new RuntimeException[]{new MontoNoPermitidoException("monto"),
                new SaldoInsuficienteException(), new CuentaInactivaException()}) {
            doThrow(e).when(transaccionService).pagarServicio(any(), any());
            mockMvc.perform(pagar(valida())).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensaje").value(e.getMessage()));
        }
    }

    @Test
    @DisplayName("Duplicado / clave reutilizada / cuenta bloqueada por otra operacion -> 409")
    void conflictos409() throws Exception {
        doThrow(new DuplicateResourceException("duplicada")).when(transaccionService).pagarServicio(any(), any());
        mockMvc.perform(pagar(valida())).andExpect(status().isConflict());

        doThrow(new CannotAcquireLockException("lock timeout en tabla cuentas")).when(transaccionService).pagarServicio(any(), any());
        MvcResult res = mockMvc.perform(pagar(valida())).andExpect(status().isConflict()).andReturn();
        assertFalse(res.getResponse().getContentAsString().contains("cuentas"));
    }

    // ---------- proveedor ----------

    @Test
    @DisplayName("Rechazo del proveedor -> 422 con solo el codigo")
    void rechazoProveedor() throws Exception {
        when(transaccionService.pagarServicio(any(), any())).thenThrow(new ProveedorRechazoException("82"));
        mockMvc.perform(pagar(valida())).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores.codigoProveedor").value("82"));
    }

    @Test
    @DisplayName("Proveedor no disponible -> 503 sin detalles internos")
    void proveedorNoDisponible() throws Exception {
        when(transaccionService.pagarServicio(any(), any()))
                .thenThrow(new ProveedorNoDisponibleException("https://interno/sendTx.do token=abc123"));
        MvcResult res = mockMvc.perform(pagar(valida())).andExpect(status().isServiceUnavailable()).andReturn();
        String cuerpo = res.getResponse().getContentAsString();
        assertFalse(cuerpo.contains("abc123"));
        assertFalse(cuerpo.contains("interno"));
    }

    @Test
    @DisplayName("Error inesperado -> 500 generico: no filtra trazas, SQL ni secretos")
    void errorInesperado() throws Exception {
        when(transaccionService.pagarServicio(any(), any()))
                .thenThrow(new IllegalStateException("select * from cuentas where password='hash' Bearer eyJhbGciOi"));
        MvcResult res = mockMvc.perform(pagar(valida())).andExpect(status().isInternalServerError()).andReturn();
        String cuerpo = res.getResponse().getContentAsString();
        assertFalse(cuerpo.contains("select"));
        assertFalse(cuerpo.contains("Bearer"));
        assertFalse(cuerpo.contains("IllegalStateException"));
    }

    // ---------- historial ----------

    @Test
    @DisplayName("POST /transacciones/historial recibe la cuenta en el cuerpo (sin PathVariable) -> 200")
    void historial() throws Exception {
        when(transaccionService.obtenerHistorial(any(), eq(CORREO))).thenReturn(List.of());
        mockMvc.perform(post("/transacciones/historial")
                        .principal(new UsernamePasswordAuthenticationToken(CORREO, null, Collections.emptyList()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"0000000001\",\"limite\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Historial: limite fuera de rango -> 400")
    void historialLimiteInvalido() throws Exception {
        mockMvc.perform(post("/transacciones/historial")
                        .principal(new UsernamePasswordAuthenticationToken(CORREO, null, Collections.emptyList()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"0000000001\",\"limite\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La ruta antigua GET /historial/{cuenta} ya no existe")
    void rutaAntiguaEliminada() throws Exception {
        mockMvc.perform(get("/transacciones/historial/0000000001")).andExpect(status().isNotFound());
    }
}
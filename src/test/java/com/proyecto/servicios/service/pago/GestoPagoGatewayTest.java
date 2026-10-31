package com.proyecto.servicios.service.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.service.pago.ResultadoProveedor.Tipo;
import com.proyecto.servicios.util.GestoPagoCryptoUtil;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestoPagoGateway: clasificacion de cada desenlace de sendTx")
class GestoPagoGatewayTest {

    private static final String KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    private static String xml(String codigo, String texto) {
        return "<?xml version='1.0' encoding='UTF-8'?><RESPONSE><ID_TX>291437188</ID_TX>"
                + "<NUM_AUTORIZACION>100024988</NUM_AUTORIZACION><MONTO>100.0</MONTO>"
                + "<MENSAJE><CODIGO>" + codigo + "</CODIGO><TEXTO>" + texto + "</TEXTO></MENSAJE></RESPONSE>";
    }

    @Mock
    private GestoPagoTxClient client;

    private GestoPagoCryptoUtil crypto;
    private GestoPagoTxProperties props;
    private GestoPagoGateway gateway;

    @BeforeEach
    void setUp() {
        crypto = new GestoPagoCryptoUtil(KEY, IV);
        props = new GestoPagoTxProperties();
        props.setUnidad("083");
        props.setApiKey("api-key-de-prueba");
        gateway = new GestoPagoGateway(client, crypto, new ObjectMapper(), props, 83);
    }

    private static Request fakeRequest() {
        return Request.create(Request.HttpMethod.POST, "https://proveedor/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    private ResultadoProveedor enviar() {
        return gateway.enviar("{\"x\":1}", "UPC_83_X");
    }

    // ---------- payload ----------

    @Test
    @DisplayName("El payload lleva el UPC, la unidad y el monto solo cuando el producto es de monto abierto")
    void construirPayload() throws Exception {
        CatalogoProducto p = new CatalogoProducto();
        p.setIdProducto("582");
        p.setIdServicio("133");

        JsonNode fijo = new ObjectMapper().readTree(gateway.construirPayload(
                new PagoPlan(p, new BigDecimal("100.00"), "5512345678", null, false), "UPC_83_A"));
        assertEquals(582, fijo.get("idProducto").asInt());
        assertEquals("UPC_83_A", fijo.get("upc").asText());
        assertEquals("083", fijo.get("unidad").asText());
        assertFalse(fijo.has("montoPago"));
        assertFalse(fijo.has("referencia"));

        JsonNode abierto = new ObjectMapper().readTree(gateway.construirPayload(
                new PagoPlan(p, new BigDecimal("250.50"), "1111111111", "REF1", true), "UPC_83_B"));
        assertEquals(250.50, abierto.get("montoPago").asDouble());
        assertEquals("REF1", abierto.get("referencia").asText());
    }

    @Test
    @DisplayName("Cada UPC generado es distinto y lleva el distribuidor")
    void upcUnico() {
        String a = gateway.generarUpc();
        String b = gateway.generarUpc();
        assertNotEquals(a, b);
        assertTrue(a.startsWith("UPC_83_"));
    }

    @Test
    @DisplayName("El payload se envia cifrado: el JSON claro nunca viaja en 'signed'")
    void enviaCifradoYConApiKey() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(xml("01", "ok")));
        enviar();

        ArgumentCaptor<String> signed = ArgumentCaptor.forClass(String.class);
        verify(client).sendTx(eq("api-key-de-prueba"), any(), signed.capture());
        assertNotEquals("{\"x\":1}", signed.getValue());
        assertEquals("{\"x\":1}", crypto.decrypt(signed.getValue()));
    }

    // ---------- respuestas del proveedor ----------

    @Test
    @DisplayName("Codigo 01 -> EXITO con idTx y numero de autorizacion")
    void exito() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(xml("01", "ok")));
        ResultadoProveedor r = enviar();
        assertEquals(Tipo.EXITO, r.tipo());
        assertEquals("291437188", r.idTx());
        assertEquals("100024988", r.numAutorizacion());
    }

    @Test
    @DisplayName("Otro codigo concluyente -> RECHAZO")
    void rechazo() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(xml("82", "Saldo insuficiente del distribuidor")));
        ResultadoProveedor r = enviar();
        assertEquals(Tipo.RECHAZO, r.tipo());
        assertEquals("82", r.codigo());
    }

    @Test
    @DisplayName("Un codigo configurado como 'en proceso' es INCIERTO, no un rechazo")
    void codigoPendienteConfigurado() {
        props.setCodigosPendientes(Set.of("99"));
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(xml("99", "En proceso")));
        assertEquals(Tipo.INCIERTO, enviar().tipo());
    }

    @Test
    @DisplayName("Cuerpo vacio, no XML o sin codigo -> INCIERTO (el proveedor pudo haber procesado)")
    void respuestasInusables() {
        for (String body : new String[]{"", "   ", "esto no es xml", "<RESPONSE></RESPONSE>", "<OTRA/>"}) {
            when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(body));
            assertEquals(Tipo.INCIERTO, enviar().tipo(), "cuerpo=[" + body + "]");
        }
    }

    @Test
    @DisplayName("Sin cuerpo -> INCIERTO")
    void sinCuerpo() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok().build());
        assertEquals(Tipo.INCIERTO, enviar().tipo());
    }

    @Test
    @DisplayName("XXE: una entidad externa en la respuesta no se resuelve ni rompe el flujo")
    void xxeNoSeResuelve() {
        String malicioso = "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY x SYSTEM 'file:///C:/Windows/win.ini'>]>"
                + "<RESPONSE><MENSAJE><CODIGO>&x;</CODIGO></MENSAJE></RESPONSE>";
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(malicioso));
        ResultadoProveedor r = enviar();
        assertEquals(Tipo.INCIERTO, r.tipo());
    }

    // ---------- fallos de transporte ----------

    @Test
    @DisplayName("Timeout de lectura -> INCIERTO: NO se asume que el proveedor no proceso")
    void timeoutLectura() {
        when(client.sendTx(any(), any(), any())).thenThrow(new RetryableException(-1, "Read timed out",
                Request.HttpMethod.POST, new SocketTimeoutException("Read timed out"), (Long) null, fakeRequest()));
        assertEquals(Tipo.INCIERTO, enviar().tipo());
    }

    @Test
    @DisplayName("Timeout al conectar -> NO_ENVIADO (no hubo transmision)")
    void timeoutConexion() {
        when(client.sendTx(any(), any(), any())).thenThrow(new RetryableException(-1, "Connect timed out",
                Request.HttpMethod.POST, new SocketTimeoutException("Connect timed out"), (Long) null, fakeRequest()));
        assertEquals(Tipo.NO_ENVIADO, enviar().tipo());
    }

    @Test
    @DisplayName("Conexion rechazada -> NO_ENVIADO")
    void conexionRechazada() {
        when(client.sendTx(any(), any(), any())).thenThrow(new RetryableException(-1, "refused",
                Request.HttpMethod.POST, new ConnectException("Connection refused"), (Long) null, fakeRequest()));
        assertEquals(Tipo.NO_ENVIADO, enviar().tipo());
    }

    @Test
    @DisplayName("Conexion cortada despues de conectar -> INCIERTO")
    void conexionCortada() {
        when(client.sendTx(any(), any(), any())).thenThrow(new RetryableException(-1, "reset",
                Request.HttpMethod.POST, new java.io.IOException("Connection reset"), (Long) null, fakeRequest()));
        assertEquals(Tipo.INCIERTO, enviar().tipo());
    }

    @Test
    @DisplayName("HTTP 5xx -> INCIERTO")
    void http5xx() {
        for (int status : new int[]{500, 502, 503, 504}) {
            doThrow(FeignException.errorStatus("sendTx",
                    feign.Response.builder().status(status).reason("x").request(fakeRequest())
                            .headers(Collections.emptyMap()).build())).when(client).sendTx(any(), any(), any());
            assertEquals(Tipo.INCIERTO, enviar().tipo(), "status=" + status);
        }
    }

    @Test
    @DisplayName("HTTP 4xx (p. ej. token vencido) -> NO_ENVIADO")
    void http4xx() {
        for (int status : new int[]{400, 401, 403}) {
            doThrow(FeignException.errorStatus("sendTx",
                    feign.Response.builder().status(status).reason("x").request(fakeRequest())
                            .headers(Collections.emptyMap()).build())).when(client).sendTx(any(), any(), any());
            assertEquals(Tipo.NO_ENVIADO, enviar().tipo(), "status=" + status);
        }
    }

    @Test
    @DisplayName("Sin token Bearer -> NO_ENVIADO, la peticion nunca sale")
    void sinToken() {
        when(client.sendTx(any(), any(), any())).thenThrow(new ProveedorNoDisponibleException("sin token"));
        assertEquals(Tipo.NO_ENVIADO, enviar().tipo());
    }

    @Test
    @DisplayName("Error inesperado desconocido -> INCIERTO")
    void errorDesconocido() {
        when(client.sendTx(any(), any(), any())).thenThrow(new IllegalStateException("boom"));
        assertEquals(Tipo.INCIERTO, enviar().tipo());
    }

    @Test
    @DisplayName("Falla de cifrado local -> NO_ENVIADO sin llamar al proveedor")
    void fallaCifrado() {
        GestoPagoGateway sinLlave = new GestoPagoGateway(client, new GestoPagoCryptoUtil("no-es-base64!", IV),
                new ObjectMapper(), props, 83);
        assertEquals(Tipo.NO_ENVIADO, sinLlave.enviar("{}", "UPC").tipo());
        org.mockito.Mockito.verifyNoInteractions(client);
    }
}
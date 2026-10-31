package com.proyecto.servicios.service.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.MontoNoPermitidoException;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.ProveedorRechazoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import com.proyecto.servicios.service.Impl.TransaccionServiceImpl;
import com.proyecto.servicios.util.GestoPagoCryptoUtil;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Flujo completo con las clases reales (validadores, ledger, gateway, cifrado, BCrypt) y SOLO los bordes
 * simulados: repositorios en memoria y el cliente HTTP de GestoPago. Verifica el estado final del saldo y de la
 * transaccion en cada escenario de fallo.
 *
 * <p>Limitacion: el bloqueo SELECT ... FOR UPDATE no se puede ejercitar sin una base de datos real.
 */
@DisplayName("Flujo completo de pago: saldo, idempotencia y fallos del proveedor")
class PagoFlujoIntegracionTest {

    private static final String CORREO = "ana@correo.com";
    private static final String PIN = "PinSeguro#2026";
    private static final String CUENTA = "0000000001";
    private static final String KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";
    private static final String XML_OK = "<RESPONSE><ID_TX>1</ID_TX><NUM_AUTORIZACION>99</NUM_AUTORIZACION>"
            + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>ok</TEXTO></MENSAJE></RESPONSE>";
    private static final String XML_RECHAZO = "<RESPONSE><MENSAJE><CODIGO>82</CODIGO><TEXTO>x</TEXTO></MENSAJE></RESPONSE>";

    private final List<Transaccion> tablaTx = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    private GestoPagoTxClient client;
    private Cuenta cuenta;
    private TransaccionServiceImpl servicio;
    private PagoLedgerService ledger;
    private TransaccionRepository txRepo;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        Cliente cliente = Cliente.builder().id(10L).activo(true).bloqueado(false).build();
        Usuario usuario = Usuario.builder().id(5L).correo(CORREO).password(encoder.encode(PIN)).activo(true).cliente(cliente).build();
        cuenta = Cuenta.builder().id(1L).numeroCuenta(CUENTA).cliente(cliente)
                .saldo(new BigDecimal("500.00")).estatus(EstatusCuenta.ACTIVA).build();

        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        when(usuarioRepo.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        CuentaRepository cuentaRepo = mock(CuentaRepository.class);
        when(cuentaRepo.findByNumeroCuenta(CUENTA)).thenReturn(Optional.of(cuenta));
        when(cuentaRepo.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(cuenta));
        when(cuentaRepo.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        txRepo = mock(TransaccionRepository.class);
        when(txRepo.save(any(Transaccion.class))).thenAnswer(i -> {
            Transaccion t = i.getArgument(0);
            if (t.getId() == null) {
                t.setId(ids.incrementAndGet());
                t.setFechaCreacion(LocalDateTime.now());
                tablaTx.add(t);
            }
            return t;
        });
        when(txRepo.findByIdForUpdate(any())).thenAnswer(i ->
                tablaTx.stream().filter(t -> t.getId().equals(i.getArgument(0))).findFirst());
        when(txRepo.findByNumeroCuentaAndClaveIdempotencia(any(), any())).thenAnswer(i ->
                tablaTx.stream().filter(t -> t.getClaveIdempotencia().equals(i.getArgument(1))).findFirst());
        when(txRepo.findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(any(), any(), any(), any()))
                .thenAnswer(i -> tablaTx.stream()
                        .filter(t -> t.getHuellaSolicitud().equals(i.getArgument(1)))
                        .filter(t -> ((java.util.Collection<?>) i.getArgument(2)).contains(t.getEstatus()))
                        .findFirst());

        CatalogoProducto fijo = new CatalogoProducto();
        fijo.setIdProducto("582");
        fijo.setIdServicio("133");
        fijo.setNombreProducto("Recarga 100");
        fijo.setTipoFront("1");
        fijo.setIdCatTipoServicio("1");
        fijo.setPrecio("100.0");
        CatalogoProductoRepository catalogo = mock(CatalogoProductoRepository.class);
        when(catalogo.findByIdProductoAndIdServicio("582", "133")).thenReturn(Optional.of(fijo));

        GestoPagoTxProperties props = new GestoPagoTxProperties();
        props.setUnidad("083");
        props.setApiKey("api-key-de-prueba");

        client = mock(GestoPagoTxClient.class);
        GestoPagoGateway gateway = new GestoPagoGateway(client, new GestoPagoCryptoUtil(KEY, IV), new ObjectMapper(), props, 83);
        ledger = new PagoLedgerService(cuentaRepo, txRepo, props);
        servicio = new TransaccionServiceImpl(
                new AutorizacionClienteValidator(usuarioRepo, cuentaRepo, encoder),
                new ProductoPagoValidator(catalogo, props), ledger, gateway, txRepo);
    }

    private PagarServicioRequestDTO pago(String clave) {
        return PagarServicioRequestDTO.builder().numeroCuenta(CUENTA).idProducto("582").idServicio("133")
                .telefonoOreferencia("5512345678").passwordOPin(PIN).claveIdempotencia(clave).build();
    }

    private static String clave(int n) {
        return String.format("00000000-0000-4000-8000-%012d", n);
    }

    private static RetryableException timeoutDeLectura() {
        Request req = Request.create(Request.HttpMethod.POST, "https://p/sendTx.do", Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "Read timed out", Request.HttpMethod.POST,
                new SocketTimeoutException("Read timed out"), (Long) null, req);
    }

    @Test
    @DisplayName("Camino feliz: se descuenta una vez y queda EXITOSA con la autorizacion del proveedor")
    void caminoFeliz() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_OK));

        TransaccionResponseDTO r = servicio.pagarServicio(pago(clave(1)), CORREO);

        assertEquals(EstatusTransaccion.EXITOSA, r.getEstatus());
        assertEquals("99", r.getNumAutorizacion());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        assertEquals(1, tablaTx.size());
        verify(client, times(1)).sendTx(any(), any(), any());
    }

    @Test
    @DisplayName("Timeout de lectura: el dinero NO se devuelve, queda retenido y pendiente de conciliacion")
    void timeoutRetieneSaldo() {
        when(client.sendTx(any(), any(), any())).thenThrow(timeoutDeLectura());

        TransaccionResponseDTO r = servicio.pagarServicio(pago(clave(1)), CORREO);

        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, r.getEstatus());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo(), "el saldo sigue descontado");
        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, tablaTx.get(0).getEstatus());
    }

    @Test
    @DisplayName("Reintento con la misma clave tras un timeout: no hay segunda llamada ni segundo cargo")
    void reintentoMismaClaveNoDuplica() {
        when(client.sendTx(any(), any(), any())).thenThrow(timeoutDeLectura());
        servicio.pagarServicio(pago(clave(1)), CORREO);

        TransaccionResponseDTO r = servicio.pagarServicio(pago(clave(1)), CORREO);

        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, r.getEstatus());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        assertEquals(1, tablaTx.size());
        verify(client, times(1)).sendTx(any(), any(), any());
    }

    @Test
    @DisplayName("Reintento con OTRA clave y mismos datos mientras hay una pendiente: 409, sin segundo cargo")
    void dobleEnvioConOtraClave() {
        when(client.sendTx(any(), any(), any())).thenThrow(timeoutDeLectura());
        servicio.pagarServicio(pago(clave(1)), CORREO);

        assertThrows(DuplicateResourceException.class, () -> servicio.pagarServicio(pago(clave(2)), CORREO));

        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        verify(client, times(1)).sendTx(any(), any(), any());
    }

    @Test
    @DisplayName("Misma clave con datos distintos: conflicto, no se reutiliza la operacion")
    void claveReutilizadaConOtroMonto() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_OK));
        servicio.pagarServicio(pago(clave(1)), CORREO);

        PagarServicioRequestDTO otra = pago(clave(1));
        otra.setTelefonoOreferencia("5599999999");
        assertThrows(DuplicateResourceException.class, () -> servicio.pagarServicio(otra, CORREO));
        verify(client, times(1)).sendTx(any(), any(), any());
    }

    @Test
    @DisplayName("Rechazo del proveedor: el saldo se devuelve integro y queda RECHAZADA")
    void rechazoDevuelveSaldo() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_RECHAZO));

        assertThrows(ProveedorRechazoException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));

        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        assertEquals(EstatusTransaccion.RECHAZADA, tablaTx.get(0).getEstatus());
    }

    @Test
    @DisplayName("Repetir una operacion rechazada con la misma clave devuelve el mismo rechazo sin volver a cobrar")
    void repeticionDeRechazo() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_RECHAZO));
        assertThrows(ProveedorRechazoException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));

        assertThrows(ProveedorRechazoException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));

        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(client, times(1)).sendTx(any(), any(), any());
    }

    @Test
    @DisplayName("Sin conexion con el proveedor: saldo devuelto, FALLIDA y 503")
    void sinConexion() {
        Request req = Request.create(Request.HttpMethod.POST, "https://p/sendTx.do", Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null);
        when(client.sendTx(any(), any(), any())).thenThrow(new RetryableException(-1, "refused", Request.HttpMethod.POST,
                new java.net.ConnectException("refused"), (Long) null, req));

        assertThrows(ProveedorNoDisponibleException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));

        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        assertEquals(EstatusTransaccion.FALLIDA, tablaTx.get(0).getEstatus());
    }

    @Test
    @DisplayName("Sin token Bearer: la peticion no sale, saldo devuelto")
    void sinToken() {
        when(client.sendTx(any(), any(), any())).thenThrow(new ProveedorNoDisponibleException("sin token"));
        assertThrows(ProveedorNoDisponibleException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("Saldo insuficiente / cuenta bloqueada: el proveedor ni se invoca")
    void noLlegaAlProveedor() {
        cuenta.setSaldo(new BigDecimal("50.00"));
        assertThrows(SaldoInsuficienteException.class, () -> servicio.pagarServicio(pago(clave(1)), CORREO));

        cuenta.setSaldo(new BigDecimal("500.00"));
        cuenta.setEstatus(EstatusCuenta.BLOQUEADA);
        assertThrows(RuntimeException.class, () -> servicio.pagarServicio(pago(clave(2)), CORREO));

        verifyNoInteractions(client);
        assertTrue(tablaTx.isEmpty());
    }

    @Test
    @DisplayName("Monto manipulado en un producto de precio fijo: rechazado antes de reservar")
    void montoManipulado() {
        PagarServicioRequestDTO r = pago(clave(1));
        r.setMonto(new BigDecimal("0.01"));
        assertThrows(MontoNoPermitidoException.class, () -> servicio.pagarServicio(r, CORREO));
        verifyNoInteractions(client);
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("PIN incorrecto o usuario que no es titular: nada se descuenta ni se envia")
    void identidad() {
        PagarServicioRequestDTO malPin = pago(clave(1));
        malPin.setPasswordOPin("otro");
        assertThrows(UnauthorizedAccessException.class, () -> servicio.pagarServicio(malPin, CORREO));

        assertThrows(AccesoDenegadoException.class, () -> servicio.pagarServicio(pago(clave(2)), "intruso@correo.com"));

        verifyNoInteractions(client);
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("Lo que viaja al proveedor va cifrado y lleva el UPC guardado en la transaccion")
    void payloadCifradoYTrazable() {
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_OK));
        servicio.pagarServicio(pago(clave(1)), CORREO);

        Transaccion tx = tablaTx.get(0);
        assertTrue(tx.getUpc().startsWith("UPC_83_"));
        assertTrue(tx.getPayloadEnviado().contains(tx.getUpc()));
        assertFalse(tx.getPayloadEnviado().contains(PIN), "el payload guardado no contiene credenciales");
    }

    @Test
    @DisplayName("Conciliacion de una operacion incierta: el mismo UPC se reenvia y se confirma sin otro descuento")
    void conciliacionConfirma() {
        when(client.sendTx(any(), any(), any())).thenThrow(timeoutDeLectura());
        servicio.pagarServicio(pago(clave(1)), CORREO);
        Transaccion tx = tablaTx.get(0);

        // La conciliacion reutiliza gateway y ledger reales
        reset(client);
        when(client.sendTx(any(), any(), any())).thenReturn(ResponseEntity.ok(XML_OK));
        GestoPagoTxProperties props = new GestoPagoTxProperties();
        props.setUnidad("083");
        props.setApiKey("k");
        props.getConciliacion().setReenvioMismoUpc(true);
        props.getConciliacion().setEsperaMinutos(1);
        GestoPagoGateway gateway = new GestoPagoGateway(client, new GestoPagoCryptoUtil(KEY, IV), new ObjectMapper(), props, 83);
        when(txRepo.findByEstatusInAndFechaActualizacionBeforeAndIntentosConciliacionLessThan(any(), any(), anyInt()))
                .thenReturn(List.of(tx));

        new ConciliacionService(txRepo, ledger, gateway, props).conciliarPendientes();

        assertEquals(EstatusTransaccion.EXITOSA, tx.getEstatus());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
    }
}
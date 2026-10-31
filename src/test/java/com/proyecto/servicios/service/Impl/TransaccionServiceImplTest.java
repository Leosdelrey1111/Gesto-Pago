package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.ProveedorRechazoException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.transaccion.HistorialTransaccionesRequestDTO;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionHistorialDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import com.proyecto.servicios.service.pago.AutorizacionClienteValidator;
import com.proyecto.servicios.service.pago.GestoPagoGateway;
import com.proyecto.servicios.service.pago.PagoLedgerService;
import com.proyecto.servicios.service.pago.PagoPlan;
import com.proyecto.servicios.service.pago.ProductoPagoValidator;
import com.proyecto.servicios.service.pago.ResultadoProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionServiceImpl: orquestacion del pago")
class TransaccionServiceImplTest {

    private static final String CORREO = "ana@correo.com";
    private static final String CLAVE = "6f1c2b3a-1111-4222-8333-444455556666";

    @Mock
    private AutorizacionClienteValidator autorizacion;
    @Mock
    private ProductoPagoValidator productoValidator;
    @Mock
    private PagoLedgerService ledger;
    @Mock
    private GestoPagoGateway gateway;
    @Mock
    private TransaccionRepository transaccionRepository;

    private TransaccionServiceImpl servicio;
    private PagarServicioRequestDTO request;
    private PagoPlan plan;
    private Transaccion tx;

    @BeforeEach
    void setUp() {
        servicio = new TransaccionServiceImpl(autorizacion, productoValidator, ledger, gateway, transaccionRepository);

        request = PagarServicioRequestDTO.builder().numeroCuenta("0000000001").idProducto("582").idServicio("133")
                .telefonoOreferencia("5512345678").passwordOPin("1234").claveIdempotencia(CLAVE).build();
        CatalogoProducto p = new CatalogoProducto();
        p.setIdProducto("582");
        p.setIdServicio("133");
        plan = new PagoPlan(p, new BigDecimal("100.00"), "5512345678", null, false);
        tx = Transaccion.builder().id(7L).numeroCuenta("0000000001").upc("UPC_83_X").claveIdempotencia(CLAVE)
                .monto(new BigDecimal("100.00")).saldoAnterior(new BigDecimal("500.00")).saldoNuevo(new BigDecimal("400.00"))
                .nombreProducto("Recarga 100").estatus(EstatusTransaccion.PENDIENTE).build();
    }

    private void flujoHastaReserva(boolean nueva) {
        when(productoValidator.validar(request)).thenReturn(plan);
        when(gateway.generarUpc()).thenReturn("UPC_83_X");
        when(gateway.construirPayload(plan, "UPC_83_X")).thenReturn("{payload}");
        when(ledger.reservar(eq(request), eq(plan), anyString(), eq("UPC_83_X"), eq("{payload}")))
                .thenReturn(new PagoLedgerService.Reserva(tx, nueva));
    }

    private Transaccion con(EstatusTransaccion estatus) {
        Transaccion t = Transaccion.builder().id(7L).numeroCuenta("0000000001").upc("UPC_83_X").claveIdempotencia(CLAVE)
                .monto(new BigDecimal("100.00")).saldoAnterior(new BigDecimal("500.00")).saldoNuevo(new BigDecimal("400.00"))
                .nombreProducto("Recarga 100").estatus(estatus).build();
        return t;
    }

    @Test
    @DisplayName("Exito: valida -> reserva -> envia -> confirma, en ese orden")
    void exito() {
        flujoHastaReserva(true);
        ResultadoProveedor ok = ResultadoProveedor.exito("01", "ok", "123", "456");
        when(gateway.enviar("{payload}", "UPC_83_X")).thenReturn(ok);
        Transaccion confirmada = con(EstatusTransaccion.EXITOSA);
        confirmada.setNumAutorizacion("456");
        when(ledger.confirmar(7L, ok)).thenReturn(confirmada);

        TransaccionResponseDTO r = servicio.pagarServicio(request, CORREO);

        assertEquals(EstatusTransaccion.EXITOSA, r.getEstatus());
        assertEquals("456", r.getNumAutorizacion());
        assertEquals(new BigDecimal("400.00"), r.getNuevoSaldo());
        InOrder orden = inOrder(autorizacion, productoValidator, ledger, gateway);
        orden.verify(autorizacion).autorizar(CORREO, "0000000001", "1234");
        orden.verify(productoValidator).validar(request);
        orden.verify(ledger).reservar(any(), any(), anyString(), anyString(), anyString());
        orden.verify(gateway).enviar("{payload}", "UPC_83_X");
        orden.verify(ledger).confirmar(7L, ok);
    }

    @Test
    @DisplayName("Sin autorizacion no se toca el catalogo, el saldo ni el proveedor")
    void autorizacionFalla() {
        doThrow(new AccesoDenegadoException("no")).when(autorizacion).autorizar(CORREO, "0000000001", "1234");
        assertThrows(AccesoDenegadoException.class, () -> servicio.pagarServicio(request, CORREO));
        verifyNoInteractions(productoValidator, ledger, gateway);
    }

    @Test
    @DisplayName("Producto o monto invalidos: nunca se reserva saldo ni se llama al proveedor")
    void validacionDeProductoFalla() {
        when(productoValidator.validar(request)).thenThrow(new ResourceNotFoundException("no existe"));
        assertThrows(ResourceNotFoundException.class, () -> servicio.pagarServicio(request, CORREO));
        verifyNoInteractions(ledger, gateway);
    }

    @Test
    @DisplayName("Saldo insuficiente en la reserva: no se llama al proveedor")
    void saldoInsuficiente() {
        when(productoValidator.validar(request)).thenReturn(plan);
        when(gateway.generarUpc()).thenReturn("UPC_83_X");
        when(gateway.construirPayload(plan, "UPC_83_X")).thenReturn("{payload}");
        when(ledger.reservar(any(), any(), anyString(), anyString(), anyString())).thenThrow(new SaldoInsuficienteException());
        assertThrows(SaldoInsuficienteException.class, () -> servicio.pagarServicio(request, CORREO));
        verify(gateway, never()).enviar(any(), any());
    }

    @Test
    @DisplayName("Repeticion con la misma clave: devuelve el resultado guardado y NO vuelve a llamar al proveedor")
    void repeticionIdempotente() {
        tx.setEstatus(EstatusTransaccion.EXITOSA);
        flujoHastaReserva(false);

        TransaccionResponseDTO r = servicio.pagarServicio(request, CORREO);

        assertEquals(EstatusTransaccion.EXITOSA, r.getEstatus());
        verify(gateway, never()).enviar(any(), any());
        verify(ledger, never()).confirmar(any(), any());
    }

    @Test
    @DisplayName("Repeticion de una operacion rechazada: mismo rechazo, sin nuevo cargo")
    void repeticionRechazada() {
        tx.setEstatus(EstatusTransaccion.RECHAZADA);
        tx.setCodigoRespuesta("82");
        flujoHastaReserva(false);
        ProveedorRechazoException ex = assertThrows(ProveedorRechazoException.class, () -> servicio.pagarServicio(request, CORREO));
        assertEquals("82", ex.getCodigoProveedor());
        verify(gateway, never()).enviar(any(), any());
    }

    @Test
    @DisplayName("Repeticion de una operacion fallida por no envio: 503 sin llamar al proveedor")
    void repeticionFallida() {
        tx.setEstatus(EstatusTransaccion.FALLIDA);
        flujoHastaReserva(false);
        assertThrows(ProveedorNoDisponibleException.class, () -> servicio.pagarServicio(request, CORREO));
        verify(gateway, never()).enviar(any(), any());
    }

    @Test
    @DisplayName("Repeticion mientras sigue pendiente: devuelve PENDIENTE_CONCILIACION sin reenviar")
    void repeticionPendiente() {
        tx.setEstatus(EstatusTransaccion.PENDIENTE_CONCILIACION);
        flujoHastaReserva(false);
        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, servicio.pagarServicio(request, CORREO).getEstatus());
        verify(gateway, never()).enviar(any(), any());
    }

    @Test
    @DisplayName("Rechazo del proveedor: devuelve el saldo (RECHAZADA) y responde 422 con el codigo")
    void rechazo() {
        flujoHastaReserva(true);
        ResultadoProveedor rechazo = ResultadoProveedor.rechazo("82", "Saldo insuficiente del distribuidor", null, null);
        when(gateway.enviar("{payload}", "UPC_83_X")).thenReturn(rechazo);
        when(ledger.devolver(7L, EstatusTransaccion.RECHAZADA, rechazo)).thenReturn(con(EstatusTransaccion.RECHAZADA));

        ProveedorRechazoException ex = assertThrows(ProveedorRechazoException.class, () -> servicio.pagarServicio(request, CORREO));

        assertEquals("82", ex.getCodigoProveedor());
        assertFalse(ex.getMessage().contains("distribuidor"), "no se reenvia el texto interno del proveedor");
        verify(ledger).devolver(7L, EstatusTransaccion.RECHAZADA, rechazo);
    }

    @Test
    @DisplayName("No enviado: devuelve el saldo (FALLIDA) y responde 503")
    void noEnviado() {
        flujoHastaReserva(true);
        ResultadoProveedor noEnviado = ResultadoProveedor.noEnviado("SIN_CONEXION");
        when(gateway.enviar("{payload}", "UPC_83_X")).thenReturn(noEnviado);
        when(ledger.devolver(7L, EstatusTransaccion.FALLIDA, noEnviado)).thenReturn(con(EstatusTransaccion.FALLIDA));

        assertThrows(ProveedorNoDisponibleException.class, () -> servicio.pagarServicio(request, CORREO));
        verify(ledger).devolver(7L, EstatusTransaccion.FALLIDA, noEnviado);
    }

    @Test
    @DisplayName("Resultado incierto (timeout): el saldo NO se devuelve; queda PENDIENTE_CONCILIACION")
    void incierto() {
        flujoHastaReserva(true);
        ResultadoProveedor incierto = ResultadoProveedor.incierto("TIMEOUT", "Sin respuesta del proveedor");
        when(gateway.enviar("{payload}", "UPC_83_X")).thenReturn(incierto);
        when(ledger.marcarPendienteConciliacion(7L, incierto)).thenReturn(con(EstatusTransaccion.PENDIENTE_CONCILIACION));

        TransaccionResponseDTO r = servicio.pagarServicio(request, CORREO);

        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, r.getEstatus());
        assertTrue(r.getMensaje().contains("misma clave"));
        verify(ledger, never()).devolver(any(), any(), any());
        verify(ledger, never()).confirmar(any(), any());
    }

    @Test
    @DisplayName("Si la base falla al registrar el resultado, el error se propaga (la conciliacion lo retoma)")
    void falloAlCerrar() {
        flujoHastaReserva(true);
        ResultadoProveedor ok = ResultadoProveedor.exito("01", "ok", "123", "456");
        when(gateway.enviar("{payload}", "UPC_83_X")).thenReturn(ok);
        when(ledger.confirmar(7L, ok)).thenThrow(new IllegalStateException("db caida"));
        assertThrows(IllegalStateException.class, () -> servicio.pagarServicio(request, CORREO));
    }

    // ---------- historial ----------

    @Test
    @DisplayName("Historial: valida titularidad, aplica limite y no expone datos internos")
    void historial() {
        HistorialTransaccionesRequestDTO req = HistorialTransaccionesRequestDTO.builder().numeroCuenta("0000000001").limite(10).build();
        tx.setPayloadEnviado("{secreto-interno}");
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("0000000001", PageRequest.of(0, 10)))
                .thenReturn(List.of(tx));

        List<TransaccionHistorialDTO> r = servicio.obtenerHistorial(req, CORREO);

        assertEquals(1, r.size());
        assertEquals("UPC_83_X", r.get(0).getUpc());
        verify(autorizacion).autorizar(CORREO, "0000000001", null);
    }

    @Test
    @DisplayName("Historial de cuenta ajena: denegado y sin consultar transacciones")
    void historialAjeno() {
        HistorialTransaccionesRequestDTO req = HistorialTransaccionesRequestDTO.builder().numeroCuenta("0000000002").build();
        doThrow(new AccesoDenegadoException("no")).when(autorizacion).autorizar(CORREO, "0000000002", null);
        assertThrows(AccesoDenegadoException.class, () -> servicio.obtenerHistorial(req, CORREO));
        verifyNoInteractions(transaccionRepository);
    }

    @Test
    @DisplayName("Historial sin limite usa el valor por defecto (50)")
    void historialLimitePorDefecto() {
        HistorialTransaccionesRequestDTO req = HistorialTransaccionesRequestDTO.builder().numeroCuenta("0000000001").build();
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("0000000001", PageRequest.of(0, 50)))
                .thenReturn(List.of());
        assertTrue(servicio.obtenerHistorial(req, CORREO).isEmpty());
    }
}
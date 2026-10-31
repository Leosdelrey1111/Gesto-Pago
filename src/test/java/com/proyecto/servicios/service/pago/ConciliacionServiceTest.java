package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConciliacionService: operaciones con resultado incierto")
class ConciliacionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private PagoLedgerService ledger;
    @Mock
    private GestoPagoGateway gateway;

    private GestoPagoTxProperties props;
    private ConciliacionService servicio;
    private Transaccion tx;

    @BeforeEach
    void setUp() {
        props = new GestoPagoTxProperties();
        servicio = new ConciliacionService(transaccionRepository, ledger, gateway, props);
        tx = Transaccion.builder().id(1L).numeroCuenta("1").upc("UPC_83_X").payloadEnviado("{p}")
                .monto(new BigDecimal("100.00")).estatus(EstatusTransaccion.PENDIENTE_CONCILIACION).intentosConciliacion(0).build();
    }

    private void hayPendientes() {
        when(transaccionRepository.findByEstatusInAndFechaActualizacionBeforeAndIntentosConciliacionLessThan(
                anyCollection(), any(), anyInt())).thenReturn(List.of(tx));
    }

    @Test
    @DisplayName("Por defecto NO reenvia al proveedor: solo alerta y conserva el saldo retenido")
    void porDefectoSoloAlerta() {
        hayPendientes();
        servicio.conciliarPendientes();
        verifyNoInteractions(gateway, ledger);
    }

    @Test
    @DisplayName("Deshabilitada: no consulta nada")
    void deshabilitada() {
        props.getConciliacion().setHabilitada(false);
        servicio.conciliarPendientes();
        verifyNoInteractions(transaccionRepository, gateway, ledger);
    }

    @Test
    @DisplayName("Reenvio habilitado: el MISMO payload/UPC; exito -> confirma")
    void reenvioExito() {
        props.getConciliacion().setReenvioMismoUpc(true);
        hayPendientes();
        ResultadoProveedor ok = ResultadoProveedor.exito("01", "ok", "1", "2");
        when(gateway.enviar("{p}", "UPC_83_X")).thenReturn(ok);
        servicio.conciliarPendientes();
        verify(ledger).confirmar(1L, ok);
    }

    @Test
    @DisplayName("Reenvio habilitado: rechazo concluyente -> devuelve el saldo")
    void reenvioRechazo() {
        props.getConciliacion().setReenvioMismoUpc(true);
        hayPendientes();
        ResultadoProveedor rechazo = ResultadoProveedor.rechazo("82", "x", null, null);
        when(gateway.enviar("{p}", "UPC_83_X")).thenReturn(rechazo);
        servicio.conciliarPendientes();
        verify(ledger).devolver(1L, EstatusTransaccion.RECHAZADA, rechazo);
    }

    @Test
    @DisplayName("Reenvio habilitado: si sigue incierto, solo cuenta el intento y NO devuelve saldo")
    void reenvioSigueIncierto() {
        props.getConciliacion().setReenvioMismoUpc(true);
        hayPendientes();
        ResultadoProveedor incierto = ResultadoProveedor.incierto("TIMEOUT", "x");
        when(gateway.enviar("{p}", "UPC_83_X")).thenReturn(incierto);
        when(ledger.registrarIntentoConciliacion(1L, incierto)).thenReturn(tx);
        servicio.conciliarPendientes();
        verify(ledger).registrarIntentoConciliacion(1L, incierto);
        verify(ledger, never()).devolver(any(), any(), any());
    }

    @Test
    @DisplayName("Un error al conciliar una transaccion no detiene las demas")
    void erroresAislados() {
        props.getConciliacion().setReenvioMismoUpc(true);
        Transaccion otra = Transaccion.builder().id(2L).upc("UPC_83_Y").payloadEnviado("{q}")
                .estatus(EstatusTransaccion.PENDIENTE).intentosConciliacion(0).build();
        when(transaccionRepository.findByEstatusInAndFechaActualizacionBeforeAndIntentosConciliacionLessThan(
                anyCollection(), any(), anyInt())).thenReturn(List.of(tx, otra));
        when(gateway.enviar("{p}", "UPC_83_X")).thenThrow(new IllegalStateException("boom"));
        ResultadoProveedor ok = ResultadoProveedor.exito("01", "ok", "1", "2");
        when(gateway.enviar("{q}", "UPC_83_Y")).thenReturn(ok);

        servicio.conciliarPendientes();

        verify(ledger).confirmar(2L, ok);
    }

    @Test
    @DisplayName("Sin payload guardado no se reenvia (revision manual)")
    void sinPayload() {
        props.getConciliacion().setReenvioMismoUpc(true);
        tx.setPayloadEnviado(null);
        hayPendientes();
        servicio.conciliarPendientes();
        verifyNoInteractions(gateway, ledger);
    }
}
package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoLedgerService: saldo, idempotencia y estados")
class PagoLedgerServiceTest {

    private static final String CUENTA = "0000000001";
    private static final String CLAVE = "6f1c2b3a-1111-4222-8333-444455556666";

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private TransaccionRepository transaccionRepository;

    private GestoPagoTxProperties props;
    private PagoLedgerService ledger;
    private Cuenta cuenta;
    private PagarServicioRequestDTO request;
    private PagoPlan plan;

    @BeforeEach
    void setUp() {
        props = new GestoPagoTxProperties();
        props.setVentanaDuplicadoSegundos(60);
        ledger = new PagoLedgerService(cuentaRepository, transaccionRepository, props);

        cuenta = Cuenta.builder().id(1L).numeroCuenta(CUENTA).saldo(new BigDecimal("500.00"))
                .estatus(EstatusCuenta.ACTIVA).build();
        request = PagarServicioRequestDTO.builder().numeroCuenta(CUENTA).idProducto("582").idServicio("133")
                .telefonoOreferencia("5512345678").claveIdempotencia(CLAVE).build();

        CatalogoProducto p = new CatalogoProducto();
        p.setIdProducto("582");
        p.setIdServicio("133");
        p.setNombreProducto("Recarga 100");
        plan = new PagoPlan(p, new BigDecimal("100.00"), "5512345678", null, false);
    }

    private void cuentaBloqueada() {
        when(cuentaRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(cuenta));
    }

    private void sinPrevios() {
        when(transaccionRepository.findByNumeroCuentaAndClaveIdempotencia(CUENTA, CLAVE)).thenReturn(Optional.empty());
        when(transaccionRepository.findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(
                eq(CUENTA), eq("H"), anyCollection(), any())).thenReturn(Optional.empty());
    }

    private Transaccion pendiente(long id) {
        return Transaccion.builder().id(id).numeroCuenta(CUENTA).upc("UPC_83_X").monto(new BigDecimal("100.00"))
                .estatus(EstatusTransaccion.PENDIENTE).intentosConciliacion(0).build();
    }

    @Test
    @DisplayName("Reservar descuenta el saldo con la cuenta bloqueada y deja la transaccion PENDIENTE")
    void reservaDescuenta() {
        cuentaBloqueada();
        sinPrevios();
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> {
            Transaccion t = i.getArgument(0);
            t.setId(7L);
            return t;
        });

        PagoLedgerService.Reserva r = ledger.reservar(request, plan, "H", "UPC_83_X", "{}");

        assertTrue(r.nueva());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        Transaccion tx = r.transaccion();
        assertEquals(EstatusTransaccion.PENDIENTE, tx.getEstatus());
        assertEquals(new BigDecimal("500.00"), tx.getSaldoAnterior());
        assertEquals(new BigDecimal("400.00"), tx.getSaldoNuevo());
        assertEquals(CLAVE, tx.getClaveIdempotencia());
        assertEquals("H", tx.getHuellaSolicitud());
        verify(cuentaRepository).save(cuenta);
        verify(cuentaRepository, never()).findByNumeroCuenta(any());
    }

    @Test
    @DisplayName("Saldo insuficiente: no descuenta ni crea transaccion")
    void saldoInsuficiente() {
        cuenta.setSaldo(new BigDecimal("99.99"));
        cuentaBloqueada();
        sinPrevios();
        assertThrows(SaldoInsuficienteException.class, () -> ledger.reservar(request, plan, "H", "U", "{}"));
        assertEquals(new BigDecimal("99.99"), cuenta.getSaldo());
        verify(transaccionRepository, never()).save(any());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saldo exacto al monto: se permite y queda en cero")
    void saldoExacto() {
        cuenta.setSaldo(new BigDecimal("100.00"));
        cuentaBloqueada();
        sinPrevios();
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));
        ledger.reservar(request, plan, "H", "U", "{}");
        assertEquals(new BigDecimal("0.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("Cuenta no ACTIVA: no opera")
    void cuentaInactiva() {
        for (EstatusCuenta e : new EstatusCuenta[]{EstatusCuenta.INACTIVA, EstatusCuenta.BLOQUEADA, EstatusCuenta.CANCELADA}) {
            cuenta.setEstatus(e);
            cuentaBloqueada();
            sinPrevios();
            assertThrows(CuentaInactivaException.class, () -> ledger.reservar(request, plan, "H", "U", "{}"), e.name());
        }
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Misma clave y mismos datos: devuelve la operacion existente sin descontar de nuevo")
    void idempotenciaMismaSolicitud() {
        cuentaBloqueada();
        Transaccion existente = pendiente(9L);
        existente.setHuellaSolicitud("H");
        when(transaccionRepository.findByNumeroCuentaAndClaveIdempotencia(CUENTA, CLAVE)).thenReturn(Optional.of(existente));

        PagoLedgerService.Reserva r = ledger.reservar(request, plan, "H", "U-NUEVO", "{}");

        assertFalse(r.nueva());
        assertSame(existente, r.transaccion());
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).save(any());
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Misma clave con datos distintos: conflicto 409")
    void idempotenciaClaveReutilizada() {
        cuentaBloqueada();
        Transaccion existente = pendiente(9L);
        existente.setHuellaSolicitud("OTRA");
        when(transaccionRepository.findByNumeroCuentaAndClaveIdempotencia(CUENTA, CLAVE)).thenReturn(Optional.of(existente));
        assertThrows(DuplicateResourceException.class, () -> ledger.reservar(request, plan, "H", "U", "{}"));
    }

    @Test
    @DisplayName("Solicitud identica reciente con otra clave: se rechaza como duplicada")
    void duplicadoEnVentana() {
        cuentaBloqueada();
        when(transaccionRepository.findByNumeroCuentaAndClaveIdempotencia(CUENTA, CLAVE)).thenReturn(Optional.empty());
        when(transaccionRepository.findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(
                eq(CUENTA), eq("H"), anyCollection(), any())).thenReturn(Optional.of(pendiente(3L)));
        assertThrows(DuplicateResourceException.class, () -> ledger.reservar(request, plan, "H", "U", "{}"));
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("Con ventana 0 no se evalua el duplicado por huella")
    void ventanaDesactivada() {
        props.setVentanaDuplicadoSegundos(0);
        cuentaBloqueada();
        when(transaccionRepository.findByNumeroCuentaAndClaveIdempotencia(CUENTA, CLAVE)).thenReturn(Optional.empty());
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));
        ledger.reservar(request, plan, "H", "U", "{}");
        verify(transaccionRepository, never())
                .findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cuenta inexistente: acceso denegado generico")
    void cuentaInexistente() {
        when(cuentaRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.empty());
        assertThrows(AccesoDenegadoException.class, () -> ledger.reservar(request, plan, "H", "U", "{}"));
    }

    // ---------- cierres ----------

    @Test
    @DisplayName("Confirmar solo registra el resultado: el saldo no vuelve a moverse")
    void confirmar() {
        Transaccion tx = pendiente(1L);
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        Transaccion r = ledger.confirmar(1L, ResultadoProveedor.exito("01", "ok", "123", "456"));

        assertEquals(EstatusTransaccion.EXITOSA, r.getEstatus());
        assertEquals("123", r.getIdTxGestoPago());
        assertEquals("456", r.getNumAutorizacion());
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Devolver acredita el monto una sola vez y cierra la transaccion")
    void devolver() {
        Transaccion tx = pendiente(1L);
        cuenta.setSaldo(new BigDecimal("400.00"));
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));
        when(cuentaRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        Transaccion r = ledger.devolver(1L, EstatusTransaccion.RECHAZADA, ResultadoProveedor.rechazo("82", "x", null, null));

        assertEquals(EstatusTransaccion.RECHAZADA, r.getEstatus());
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }

    @Test
    @DisplayName("Devolver sobre una transaccion ya cerrada es no-op: no hay doble acreditacion")
    void devolverEsIdempotente() {
        Transaccion tx = pendiente(1L);
        tx.setEstatus(EstatusTransaccion.RECHAZADA);
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));

        ledger.devolver(1L, EstatusTransaccion.RECHAZADA, ResultadoProveedor.rechazo("82", "x", null, null));

        verifyNoInteractions(cuentaRepository);
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una transaccion EXITOSA no puede devolverse ni cambiar de estado")
    void exitosaEsFinal() {
        Transaccion tx = pendiente(1L);
        tx.setEstatus(EstatusTransaccion.EXITOSA);
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));

        Transaccion r = ledger.devolver(1L, EstatusTransaccion.FALLIDA, ResultadoProveedor.noEnviado("x"));

        assertEquals(EstatusTransaccion.EXITOSA, r.getEstatus());
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Devolver con un estatus que no es de cierre por devolucion es un error de programacion")
    void devolverEstatusInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> ledger.devolver(1L, EstatusTransaccion.EXITOSA, ResultadoProveedor.noEnviado("x")));
    }

    @Test
    @DisplayName("Resultado incierto: el saldo permanece retenido y queda PENDIENTE_CONCILIACION")
    void pendienteConciliacion() {
        Transaccion tx = pendiente(1L);
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        Transaccion r = ledger.marcarPendienteConciliacion(1L, ResultadoProveedor.incierto("TIMEOUT", "sin respuesta"));

        assertEquals(EstatusTransaccion.PENDIENTE_CONCILIACION, r.getEstatus());
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Registrar intento de conciliacion incrementa el contador")
    void intentoConciliacion() {
        Transaccion tx = pendiente(1L);
        tx.setEstatus(EstatusTransaccion.PENDIENTE_CONCILIACION);
        tx.setIntentosConciliacion(2);
        when(transaccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(3, ledger.registrarIntentoConciliacion(1L, ResultadoProveedor.incierto("TIMEOUT", "x")).getIntentosConciliacion());
    }
}
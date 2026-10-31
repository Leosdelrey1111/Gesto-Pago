package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.DuplicateResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Libro de movimientos del saldo. Cada metodo es UNA transaccion de base de datos corta y se invoca desde
 * el orquestador, que nunca mantiene una transaccion abierta durante la llamada HTTP al proveedor.
 *
 * <p>Estrategia: reservar (descontar) antes de llamar y devolver si el proveedor no proceso. Asi el saldo
 * nunca queda disponible mientras el resultado es desconocido, y un fallo del proveedor no puede dejar
 * un cobro sin descuento.
 */
@Slf4j
@Service
public class PagoLedgerService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final GestoPagoTxProperties properties;

    public PagoLedgerService(CuentaRepository cuentaRepository,
                             TransaccionRepository transaccionRepository,
                             GestoPagoTxProperties properties) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.properties = properties;
    }

    /**
     * @param transaccion la transaccion creada o la ya existente para esa clave de idempotencia
     * @param nueva       {@code false} si es una repeticion: no debe volver a llamarse al proveedor
     */
    public record Reserva(Transaccion transaccion, boolean nueva) {
    }

    /**
     * Valida con la cuenta bloqueada (SELECT ... FOR UPDATE) y descuenta el monto, dejando la transaccion en PENDIENTE.
     * El bloqueo serializa pagos simultaneos de la misma cuenta, por lo que el saldo no se puede gastar dos veces.
     */
    @Transactional
    public Reserva reservar(PagarServicioRequestDTO request, PagoPlan plan, String huella, String upc, String payload) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(request.getNumeroCuenta())
                .orElseThrow(() -> new AccesoDenegadoException("No tiene permiso para operar esta cuenta"));

        // 1) Idempotencia: la misma clave devuelve la misma operacion y nunca genera un segundo cargo.
        var existente = transaccionRepository
                .findByNumeroCuentaAndClaveIdempotencia(cuenta.getNumeroCuenta(), request.getClaveIdempotencia());
        if (existente.isPresent()) {
            if (!huella.equals(existente.get().getHuellaSolicitud())) {
                throw new DuplicateResourceException(
                        "La clave de idempotencia ya fue usada con datos distintos; genere una clave nueva");
            }
            return new Reserva(existente.get(), false);
        }

        // 2) Doble envio con claves distintas (doble clic, reintento del front sin conservar la clave).
        int ventana = properties.getVentanaDuplicadoSegundos();
        if (ventana > 0 && transaccionRepository
                .findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(
                        cuenta.getNumeroCuenta(), huella,
                        EnumSet.of(EstatusTransaccion.PENDIENTE, EstatusTransaccion.PENDIENTE_CONCILIACION,
                                EstatusTransaccion.EXITOSA),
                        LocalDateTime.now().minusSeconds(ventana))
                .isPresent()) {
            throw new DuplicateResourceException(
                    "Se detecto una solicitud identica reciente; espere unos segundos antes de repetirla");
        }

        // 3) Reglas de la cuenta, evaluadas con el saldo real bloqueado.
        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new CuentaInactivaException();
        }
        BigDecimal saldoAnterior = cuenta.getSaldo();
        if (saldoAnterior.compareTo(plan.monto()) < 0) {
            throw new SaldoInsuficienteException();
        }

        BigDecimal saldoNuevo = saldoAnterior.subtract(plan.monto());
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        Transaccion tx = transaccionRepository.save(Transaccion.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .upc(upc)
                .claveIdempotencia(request.getClaveIdempotencia())
                .huellaSolicitud(huella)
                .idProducto(plan.producto().getIdProducto())
                .idServicio(plan.producto().getIdServicio())
                .nombreProducto(plan.producto().getNombreProducto())
                .telefono(plan.telefono())
                .referencia(plan.referencia())
                .monto(plan.monto())
                .saldoAnterior(saldoAnterior)
                .saldoNuevo(saldoNuevo)
                .payloadEnviado(payload)
                .estatus(EstatusTransaccion.PENDIENTE)
                .build());
        log.info("Saldo reservado: tx={}, upc={}", tx.getId(), tx.getUpc());
        return new Reserva(tx, true);
    }

    /** El proveedor confirmo. El saldo ya estaba descontado: solo se registra el resultado. */
    @Transactional
    public Transaccion confirmar(Long idTransaccion, ResultadoProveedor resultado) {
        Transaccion tx = bloquear(idTransaccion);
        if (tx.getEstatus().esFinal()) {
            return tx;
        }
        tx.setEstatus(EstatusTransaccion.EXITOSA);
        registrarRespuesta(tx, resultado);
        log.info("Transaccion confirmada: tx={}, upc={}", tx.getId(), tx.getUpc());
        return transaccionRepository.save(tx);
    }

    /**
     * El proveedor NO proceso la operacion: se devuelve el monto y se cierra la transaccion.
     * Es idempotente: una transaccion ya cerrada no vuelve a acreditarse.
     */
    @Transactional
    public Transaccion devolver(Long idTransaccion, EstatusTransaccion estatusFinal, ResultadoProveedor resultado) {
        if (estatusFinal != EstatusTransaccion.RECHAZADA && estatusFinal != EstatusTransaccion.FALLIDA) {
            throw new IllegalArgumentException("Estatus final invalido para una devolucion: " + estatusFinal);
        }
        Transaccion tx = bloquear(idTransaccion);
        if (tx.getEstatus().esFinal()) {
            return tx;
        }
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getNumeroCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta de la transaccion no encontrada"));
        cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
        cuentaRepository.save(cuenta);

        tx.setEstatus(estatusFinal);
        registrarRespuesta(tx, resultado);
        log.info("Saldo devuelto: tx={}, upc={}, estatus={}", tx.getId(), tx.getUpc(), estatusFinal);
        return transaccionRepository.save(tx);
    }

    /** Resultado desconocido: el saldo permanece retenido hasta conciliar. */
    @Transactional
    public Transaccion marcarPendienteConciliacion(Long idTransaccion, ResultadoProveedor resultado) {
        Transaccion tx = bloquear(idTransaccion);
        if (tx.getEstatus().esFinal()) {
            return tx;
        }
        tx.setEstatus(EstatusTransaccion.PENDIENTE_CONCILIACION);
        tx.setCodigoRespuesta(resultado.codigo());
        tx.setMensajeRespuesta(resultado.texto());
        log.warn("Transaccion pendiente de conciliacion: tx={}, upc={}, motivo={}",
                tx.getId(), tx.getUpc(), resultado.codigo());
        return transaccionRepository.save(tx);
    }

    /** Cuenta un intento de conciliacion sin cambiar el estatus. */
    @Transactional
    public Transaccion registrarIntentoConciliacion(Long idTransaccion, ResultadoProveedor resultado) {
        Transaccion tx = bloquear(idTransaccion);
        if (tx.getEstatus().esFinal()) {
            return tx;
        }
        tx.setEstatus(EstatusTransaccion.PENDIENTE_CONCILIACION);
        tx.setIntentosConciliacion(tx.getIntentosConciliacion() + 1);
        tx.setCodigoRespuesta(resultado.codigo());
        tx.setMensajeRespuesta(resultado.texto());
        return transaccionRepository.save(tx);
    }

    private Transaccion bloquear(Long idTransaccion) {
        return transaccionRepository.findByIdForUpdate(idTransaccion)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada"));
    }

    private static void registrarRespuesta(Transaccion tx, ResultadoProveedor resultado) {
        tx.setCodigoRespuesta(resultado.codigo());
        tx.setMensajeRespuesta(resultado.texto());
        tx.setIdTxGestoPago(resultado.idTx());
        tx.setNumAutorizacion(resultado.numAutorizacion());
    }
}
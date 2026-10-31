package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Resuelve operaciones cuyo resultado se desconoce (timeout, 5xx, respuesta ilegible o caida de la aplicacion
 * entre la llamada y el registro del resultado).
 *
 * <p>Mientras una operacion esta pendiente el saldo permanece retenido: ni se devuelve ni se confirma por suposicion.
 *
 * <p>Con {@code reenvio-mismo-upc=false} (por defecto) el proceso solo alerta, porque el contrato de GestoPago
 * entregado no documenta una consulta de estatus ni que reenviar el mismo UPC sea idempotente. Con
 * {@code true} reenvia el MISMO payload y UPC; solo debe activarse con confirmacion escrita del proveedor, ya que
 * si el proveedor respondiera "UPC duplicado" como rechazo, se devolveria saldo de una operacion exitosa.
 */
@Slf4j
@Service
public class ConciliacionService {

    private final TransaccionRepository transaccionRepository;
    private final PagoLedgerService ledger;
    private final GestoPagoGateway gateway;
    private final GestoPagoTxProperties properties;

    public ConciliacionService(TransaccionRepository transaccionRepository,
                               PagoLedgerService ledger,
                               GestoPagoGateway gateway,
                               GestoPagoTxProperties properties) {
        this.transaccionRepository = transaccionRepository;
        this.ledger = ledger;
        this.gateway = gateway;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${gestopago.tx.conciliacion.intervalo-ms:300000}",
            initialDelayString = "${gestopago.tx.conciliacion.intervalo-ms:300000}")
    public void conciliarPendientes() {
        GestoPagoTxProperties.Conciliacion config = properties.getConciliacion();
        if (!config.isHabilitada()) {
            return;
        }

        List<Transaccion> pendientes = transaccionRepository
                .findByEstatusInAndFechaActualizacionBeforeAndIntentosConciliacionLessThan(
                        EnumSet.of(EstatusTransaccion.PENDIENTE, EstatusTransaccion.PENDIENTE_CONCILIACION),
                        LocalDateTime.now().minusMinutes(config.getEsperaMinutos()),
                        config.getMaxIntentos());
        if (pendientes.isEmpty()) {
            return;
        }

        if (!config.isReenvioMismoUpc()) {
            log.warn("{} transaccion(es) con resultado incierto requieren conciliacion manual con GestoPago. UPC: {}",
                    pendientes.size(), pendientes.stream().map(Transaccion::getUpc).toList());
            return;
        }
        pendientes.forEach(this::conciliar);
    }

    void conciliar(Transaccion tx) {
        try {
            if (tx.getPayloadEnviado() == null) {
                log.error("Transaccion sin payload para reenviar, requiere revision manual: tx={}", tx.getId());
                return;
            }
            ResultadoProveedor resultado = gateway.enviar(tx.getPayloadEnviado(), tx.getUpc());
            switch (resultado.tipo()) {
                case EXITO -> ledger.confirmar(tx.getId(), resultado);
                case RECHAZO -> ledger.devolver(tx.getId(), EstatusTransaccion.RECHAZADA, resultado);
                case NO_ENVIADO, INCIERTO -> {
                    Transaccion actualizada = ledger.registrarIntentoConciliacion(tx.getId(), resultado);
                    if (actualizada.getIntentosConciliacion() >= properties.getConciliacion().getMaxIntentos()) {
                        log.error("Conciliacion agotada, requiere revision manual: tx={}, upc={}",
                                tx.getId(), tx.getUpc());
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("Error al conciliar tx={}, upc={}", tx.getId(), tx.getUpc(), e);
        }
    }
}
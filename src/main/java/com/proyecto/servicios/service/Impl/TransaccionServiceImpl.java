package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.ProveedorRechazoException;
import com.proyecto.servicios.model.transaccion.HistorialTransaccionesRequestDTO;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionHistorialDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;
import com.proyecto.servicios.repository.transaccion.TransaccionRepository;
import com.proyecto.servicios.service.TransaccionService;
import com.proyecto.servicios.service.pago.AutorizacionClienteValidator;
import com.proyecto.servicios.service.pago.GestoPagoGateway;
import com.proyecto.servicios.service.pago.PagoLedgerService;
import com.proyecto.servicios.service.pago.PagoPlan;
import com.proyecto.servicios.service.pago.ProductoPagoValidator;
import com.proyecto.servicios.service.pago.ResultadoProveedor;
import com.proyecto.servicios.util.HuellaSolicitud;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquesta el pago. Esta clase NO es transaccional a proposito: cada paso de base de datos es una transaccion
 * corta en {@link PagoLedgerService} y la llamada HTTP al proveedor ocurre fuera de cualquier transaccion
 * (no se retienen conexiones ni bloqueos mientras se espera a un tercero).
 *
 * <pre>
 * 1. Identidad y titularidad de la cuenta + PIN   (AutorizacionClienteValidator)
 * 2. Producto, servicio y monto contra catalogo   (ProductoPagoValidator)
 * 3. Idempotencia + duplicados + cuenta + saldo + descuento, bajo bloqueo   (PagoLedgerService.reservar)
 * 4. Llamada a sendTx.do                          (GestoPagoGateway)
 * 5. Cierre segun el resultado: confirmar | devolver | dejar pendiente de conciliacion
 * </pre>
 */
@Slf4j
@Service
public class TransaccionServiceImpl implements TransaccionService {

    private static final int LIMITE_HISTORIAL_POR_DEFECTO = 50;

    private final AutorizacionClienteValidator autorizacionValidator;
    private final ProductoPagoValidator productoValidator;
    private final PagoLedgerService ledger;
    private final GestoPagoGateway gateway;
    private final TransaccionRepository transaccionRepository;
    private final com.proyecto.servicios.service.EmailService emailService;

    public TransaccionServiceImpl(AutorizacionClienteValidator autorizacionValidator,
                                  ProductoPagoValidator productoValidator,
                                  PagoLedgerService ledger,
                                  GestoPagoGateway gateway,
                                  TransaccionRepository transaccionRepository,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false)
                                  com.proyecto.servicios.service.EmailService emailService) {
        this.autorizacionValidator = autorizacionValidator;
        this.productoValidator = productoValidator;
        this.ledger = ledger;
        this.gateway = gateway;
        this.transaccionRepository = transaccionRepository;
        this.emailService = emailService;
    }

    @Override
    public TransaccionResponseDTO pagarServicio(PagarServicioRequestDTO request, String correoAutenticado) {
        autorizacionValidator.autorizar(correoAutenticado, request.getNumeroCuenta(), request.getPasswordOPin());
        PagoPlan plan = productoValidator.validar(request);

        String huella = HuellaSolicitud.de(request);
        String upc = gateway.generarUpc();
        String payload = gateway.construirPayload(plan, upc);

        PagoLedgerService.Reserva reserva = ledger.reservar(request, plan, huella, upc, payload);
        if (!reserva.nueva()) {
            log.info("Solicitud repetida con la misma clave de idempotencia: tx={}", reserva.transaccion().getId());
            return reproducir(reserva.transaccion());
        }

        Transaccion tx = reserva.transaccion();
        ResultadoProveedor resultado = gateway.enviar(payload, tx.getUpc());

        Transaccion cerrada = cerrar(tx, resultado);

        // Envío asíncrono del ticket por correo electrónico (aprobado o rechazado)
        notificarTicketPorCorreo(cerrada, correoAutenticado);

        return switch (resultado.tipo()) {
            case EXITO, INCIERTO -> aRespuesta(cerrada);
            case RECHAZO -> throw new ProveedorRechazoException(resultado.codigo());
            case NO_ENVIADO -> throw new ProveedorNoDisponibleException("El proveedor no recibio la solicitud");
        };
    }

    private void notificarTicketPorCorreo(Transaccion tx, String correoAutenticado) {
        if (emailService == null || correoAutenticado == null || correoAutenticado.trim().isEmpty()) {
            log.info("Omitiendo notificación de ticket: emailService={}, correoAutenticado={}", emailService != null, correoAutenticado);
            return;
        }
        try {
            com.proyecto.servicios.model.transaccion.TicketResponseDTO ticket = construirTicketDesdeTransaccion(tx);
            log.info("Enviando comprobante de pago por correo a {} para folio #{}", correoAutenticado, tx.getId());
            emailService.enviarTicketComprobante(correoAutenticado, ticket);
        } catch (Exception e) {
            log.warn("No fue posible enviar comprobante por correo para tx={}: {}", tx.getId(), e.getMessage(), e);
        }
    }

    private com.proyecto.servicios.model.transaccion.TicketResponseDTO construirTicketDesdeTransaccion(Transaccion tx) {
        String tituloComprobante = switch (tx.getEstatus()) {
            case EXITOSA -> "COMPROBANTE OFICIAL DE PAGO";
            case RECHAZADA -> "COMPROBANTE DE PAGO RECHAZADO";
            case FALLIDA -> "COMPROBANTE DE OPERACIÓN NO ENVIADA";
            case PENDIENTE_CONCILIACION, PENDIENTE -> "COMPROBANTE DE PAGO EN PROCESO";
        };

        String leyendaComprobante = switch (tx.getEstatus()) {
            case EXITOSA -> "Transacción procesada y liquidada a través de GestoPago. Conserve este comprobante.";
            case RECHAZADA -> "La operación fue rechazada por el proveedor. No se aplicó ningún cargo a su cuenta.";
            case FALLIDA -> "El proveedor no recibió la solicitud o no estuvo disponible. Su saldo fue protegido y devuelto.";
            case PENDIENTE_CONCILIACION, PENDIENTE -> "La transacción se encuentra en validación.";
        };

        String estatusTexto = switch (tx.getEstatus()) {
            case EXITOSA -> "PAGADO / APROBADO";
            case RECHAZADA -> "RECHAZADA";
            case FALLIDA -> "FALLIDA";
            case PENDIENTE_CONCILIACION -> "EN CONCILIACIÓN";
            case PENDIENTE -> "PENDIENTE";
        };

        return com.proyecto.servicios.model.transaccion.TicketResponseDTO.builder()
                .titulo(tituloComprobante)
                .folio(tx.getId())
                .upc(tx.getUpc())
                .numeroAutorizacion(tx.getNumAutorizacion() != null ? tx.getNumAutorizacion() : "N/A")
                .idTransaccionProveedor(tx.getIdTxGestoPago() != null ? tx.getIdTxGestoPago() : "N/A")
                .servicio(tx.getIdServicio())
                .producto(tx.getNombreProducto())
                .referenciaOTelefono(tx.getTelefono() != null ? tx.getTelefono() : tx.getReferencia())
                .monto(tx.getMonto())
                .numeroCuenta(tx.getNumeroCuenta())
                .fechaOperacion(tx.getFechaCreacion())
                .estatus(estatusTexto)
                .leyenda(leyendaComprobante)
                .emisor("GestoPagos Red Digital")
                .build();
    }

    @Override
    public List<TransaccionHistorialDTO> obtenerHistorial(HistorialTransaccionesRequestDTO request,
                                                          String correoAutenticado) {
        // Sin PIN: es una consulta. La titularidad se valida igual para evitar leer cuentas ajenas (IDOR).
        autorizacionValidator.autorizar(correoAutenticado, request.getNumeroCuenta(), null);
        int limite = request.getLimite() == null ? LIMITE_HISTORIAL_POR_DEFECTO : request.getLimite();
        return transaccionRepository
                .findByNumeroCuentaOrderByFechaCreacionDesc(request.getNumeroCuenta(), PageRequest.of(0, limite))
                .stream()
                .map(TransaccionServiceImpl::aHistorial)
                .toList();
    }

    /**
     * Persiste el resultado. Si la base de datos falla justo aqui, la transaccion queda PENDIENTE y el proceso de
     * conciliacion la retoma; por eso se deja constancia de lo que respondio el proveedor.
     */
    private Transaccion cerrar(Transaccion tx, ResultadoProveedor resultado) {
        try {
            return switch (resultado.tipo()) {
                case EXITO -> ledger.confirmar(tx.getId(), resultado);
                case RECHAZO -> ledger.devolver(tx.getId(), EstatusTransaccion.RECHAZADA, resultado);
                case NO_ENVIADO -> ledger.devolver(tx.getId(), EstatusTransaccion.FALLIDA, resultado);
                case INCIERTO -> ledger.marcarPendienteConciliacion(tx.getId(), resultado);
            };
        } catch (RuntimeException e) {
            log.error("CRITICO: no se pudo registrar el resultado. tx={}, upc={}, resultado={}, codigo={}, idTx={}, "
                            + "numAutorizacion={}. Requiere conciliacion.",
                    tx.getId(), tx.getUpc(), resultado.tipo(), resultado.codigo(), resultado.idTx(),
                    resultado.numAutorizacion(), e);
            throw e;
        }
    }

    /** Una repeticion devuelve el mismo desenlace que la solicitud original, sin tocar saldo ni proveedor. */
    private TransaccionResponseDTO reproducir(Transaccion tx) {
        return switch (tx.getEstatus()) {
            case RECHAZADA -> throw new ProveedorRechazoException(tx.getCodigoRespuesta());
            case FALLIDA -> throw new ProveedorNoDisponibleException("El proveedor no recibio la solicitud original");
            default -> aRespuesta(tx);
        };
    }

    private static TransaccionResponseDTO aRespuesta(Transaccion tx) {
        return TransaccionResponseDTO.builder()
                .folioTransaccion(tx.getId())
                .numeroCuenta(tx.getNumeroCuenta())
                .claveIdempotencia(tx.getClaveIdempotencia())
                .upc(tx.getUpc())
                .idTxGestoPago(tx.getIdTxGestoPago())
                .numAutorizacion(tx.getNumAutorizacion())
                .producto(tx.getNombreProducto())
                .monto(tx.getMonto())
                .saldoAnterior(tx.getSaldoAnterior())
                .nuevoSaldo(tx.getSaldoNuevo())
                .fecha(tx.getFechaCreacion())
                .estatus(tx.getEstatus())
                .mensaje(mensajePara(tx.getEstatus()))
                .build();
    }

    private static String mensajePara(EstatusTransaccion estatus) {
        return switch (estatus) {
            case EXITOSA -> "Pago realizado correctamente";
            case PENDIENTE, PENDIENTE_CONCILIACION ->
                    "La operacion esta en proceso de confirmacion. El monto permanece retenido; no la repita con otra clave. "
                            + "Reenvie la misma solicitud con la misma clave para consultar el resultado.";
            case RECHAZADA -> "El proveedor rechazo la operacion";
            case FALLIDA -> "La operacion no pudo enviarse al proveedor";
        };
    }

    private static TransaccionHistorialDTO aHistorial(Transaccion tx) {
        return TransaccionHistorialDTO.builder()
                .folioTransaccion(tx.getId())
                .upc(tx.getUpc())
                .producto(tx.getNombreProducto())
                .monto(tx.getMonto())
                .estatus(tx.getEstatus())
                .numAutorizacion(tx.getNumAutorizacion())
                .fecha(tx.getFechaCreacion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.proyecto.servicios.model.transaccion.TicketResponseDTO generarTicket(Long folioTransaccion, String correoAutenticado) {
        Transaccion tx = transaccionRepository.findById(folioTransaccion)
                .orElseThrow(() -> new com.proyecto.servicios.exception.ResourceNotFoundException("Transacción no encontrada con folio: " + folioTransaccion));

        // Validar titularidad para evitar acceso a tickets ajenos (Anti-IDOR)
        autorizacionValidator.autorizar(correoAutenticado, tx.getNumeroCuenta(), null);

        String tituloComprobante = switch (tx.getEstatus()) {
            case EXITOSA -> "COMPROBANTE OFICIAL DE PAGO";
            case RECHAZADA -> "COMPROBANTE DE PAGO RECHAZADO";
            case FALLIDA -> "COMPROBANTE DE OPERACIÓN FALLIDA";
            case PENDIENTE_CONCILIACION, PENDIENTE -> "COMPROBANTE DE PAGO EN PROCESO";
        };

        String leyendaComprobante = switch (tx.getEstatus()) {
            case EXITOSA -> "Transacción procesada y liquidada a través de GestoPago. Conserve este comprobante.";
            case RECHAZADA -> "La operación fue rechazada por el proveedor. No se aplicó ningún cargo a su cuenta.";
            case FALLIDA -> "No fue posible conectar con el proveedor. Su saldo no fue debitado.";
            case PENDIENTE_CONCILIACION, PENDIENTE -> "La transacción se encuentra en validación.";
        };

        String estatusTexto = switch (tx.getEstatus()) {
            case EXITOSA -> "PAGADO / APROBADO";
            case RECHAZADA -> "RECHAZADA";
            case FALLIDA -> "FALLIDA";
            case PENDIENTE_CONCILIACION -> "EN CONCILIACIÓN";
            case PENDIENTE -> "PENDIENTE";
        };

        return com.proyecto.servicios.model.transaccion.TicketResponseDTO.builder()
                .titulo(tituloComprobante)
                .folio(tx.getId())
                .upc(tx.getUpc())
                .numeroAutorizacion(tx.getNumAutorizacion() != null ? tx.getNumAutorizacion() : "N/A")
                .idTransaccionProveedor(tx.getIdTxGestoPago() != null ? tx.getIdTxGestoPago() : "N/A")
                .servicio(tx.getIdServicio())
                .producto(tx.getNombreProducto())
                .referenciaOTelefono(tx.getTelefono() != null ? tx.getTelefono() : tx.getReferencia())
                .monto(tx.getMonto())
                .numeroCuenta(tx.getNumeroCuenta())
                .fechaOperacion(tx.getFechaCreacion())
                .estatus(estatusTexto)
                .leyenda(leyendaComprobante)
                .emisor("GestoPagos Red Digital")
                .build();
    }
}
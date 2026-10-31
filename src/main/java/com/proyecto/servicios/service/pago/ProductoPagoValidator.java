package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.exception.MontoNoPermitidoException;
import com.proyecto.servicios.exception.OperacionNoProcesableException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Valida producto, servicio, destino y monto contra el catalogo de GestoPago. El monto que se cobra y se envia
 * lo decide este validador a partir del catalogo y de los limites configurados, no el cliente.
 */
@Component
public class ProductoPagoValidator {

    private static final String TIPO_FRONT_PRECIO_FIJO = "1";
    private static final String TIPO_FRONT_MONTO_ABIERTO = "2";

    private final CatalogoProductoRepository catalogoProductoRepository;
    private final GestoPagoTxProperties properties;

    public ProductoPagoValidator(CatalogoProductoRepository catalogoProductoRepository,
                                 GestoPagoTxProperties properties) {
        this.catalogoProductoRepository = catalogoProductoRepository;
        this.properties = properties;
    }

    public PagoPlan validar(PagarServicioRequestDTO request) {
        CatalogoProducto producto = catalogoProductoRepository
                .findByIdProductoAndIdServicio(request.getIdProducto(), request.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto o servicio solicitado no existe en el catalogo"));

        String tipoFront = producto.getTipoFront() == null ? TIPO_FRONT_PRECIO_FIJO : producto.getTipoFront().trim();
        boolean montoAbierto = !TIPO_FRONT_PRECIO_FIJO.equals(tipoFront);

        BigDecimal monto = montoAbierto
                ? validarMontoAbierto(request.getMonto())
                : validarPrecioFijo(producto, request.getMonto());

        String idCatTipoServicio = producto.getIdCatTipoServicio() == null ? "" : producto.getIdCatTipoServicio().trim();
        boolean pagoDeReferencia = "10".equals(idCatTipoServicio) || "11".equals(idCatTipoServicio);

        if (pagoDeReferencia) {
            return new PagoPlan(producto, monto, "#1111111111", request.getTelefonoOreferencia(), montoAbierto);
        }
        if (TIPO_FRONT_MONTO_ABIERTO.equals(tipoFront)) {
            return new PagoPlan(producto, monto, "1111111111", request.getTelefonoOreferencia(), montoAbierto);
        }
        if (!request.getTelefonoOreferencia().matches("^\\d{10}$")) {
            throw new ReglaNegocioException("El telefono debe contener exactamente 10 digitos");
        }
        return new PagoPlan(producto, monto, request.getTelefonoOreferencia(), null, montoAbierto);
    }

    private BigDecimal validarPrecioFijo(CatalogoProducto producto, BigDecimal montoSolicitado) {
        BigDecimal precio = parsearPrecio(producto.getPrecio());
        if (montoSolicitado != null && montoSolicitado.compareTo(precio) != 0) {
            throw new MontoNoPermitidoException("El monto enviado no coincide con el precio vigente del producto ("
                    + precio.toPlainString() + ")");
        }
        return precio;
    }

    private BigDecimal validarMontoAbierto(BigDecimal montoSolicitado) {
        if (montoSolicitado == null) {
            throw new MontoNoPermitidoException("Este servicio requiere indicar el monto a pagar");
        }
        if (montoSolicitado.scale() > 2 && montoSolicitado.stripTrailingZeros().scale() > 2) {
            throw new MontoNoPermitidoException("El monto admite como maximo 2 decimales");
        }
        BigDecimal monto = montoSolicitado.setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (monto.compareTo(properties.getMontoMinimo()) < 0 || monto.compareTo(properties.getMontoMaximo()) > 0) {
            throw new MontoNoPermitidoException("El monto debe estar entre " + properties.getMontoMinimo().toPlainString()
                    + " y " + properties.getMontoMaximo().toPlainString());
        }
        return monto;
    }

    private BigDecimal parsearPrecio(String precio) {
        if (precio == null || precio.isBlank()) {
            throw new OperacionNoProcesableException("El producto no tiene un precio configurado en el catalogo");
        }
        try {
            BigDecimal valor = new BigDecimal(precio.trim());
            if (valor.signum() <= 0) {
                throw new OperacionNoProcesableException("El producto tiene un precio invalido en el catalogo");
            }
            return valor.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new OperacionNoProcesableException("El producto tiene un precio invalido en el catalogo");
        }
    }
}
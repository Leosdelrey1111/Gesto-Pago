package com.proyecto.servicios.util;

import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Huella SHA-256 de los datos de negocio de un pago (cuenta, producto, servicio, destino y monto).
 * No incluye el PIN ni la clave de idempotencia.
 */
public final class HuellaSolicitud {

    private HuellaSolicitud() {
    }

    public static String de(PagarServicioRequestDTO request) {
        String monto = request.getMonto() == null ? "" : request.getMonto().stripTrailingZeros().toPlainString();
        String canonica = String.join("|",
                request.getNumeroCuenta(),
                request.getIdProducto(),
                request.getIdServicio(),
                request.getTelefonoOreferencia(),
                monto);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonica.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
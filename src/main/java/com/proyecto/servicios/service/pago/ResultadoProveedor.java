package com.proyecto.servicios.service.pago;

/**
 * Resultado interpretado de una llamada a sendTx.do.
 *
 * @param tipo   que se puede afirmar sobre el cargo
 * @param codigo codigo del proveedor, si lo hubo
 * @param texto  texto del proveedor (solo para auditoria interna, nunca para el cliente)
 */
public record ResultadoProveedor(Tipo tipo, String codigo, String texto, String idTx, String numAutorizacion) {

    public enum Tipo {
        /** El proveedor confirmo la operacion (codigo 01). */
        EXITO,
        /** El proveedor respondio de forma concluyente que no la proceso. */
        RECHAZO,
        /** La solicitud nunca llego al proveedor o este la descarto antes de procesarla. */
        NO_ENVIADO,
        /** No se puede saber si el proveedor proceso la operacion (timeout, 5xx, respuesta ilegible). */
        INCIERTO
    }

    public static ResultadoProveedor exito(String codigo, String texto, String idTx, String numAutorizacion) {
        return new ResultadoProveedor(Tipo.EXITO, codigo, texto, idTx, numAutorizacion);
    }

    public static ResultadoProveedor rechazo(String codigo, String texto, String idTx, String numAutorizacion) {
        return new ResultadoProveedor(Tipo.RECHAZO, codigo, texto, idTx, numAutorizacion);
    }

    public static ResultadoProveedor noEnviado(String motivo) {
        return new ResultadoProveedor(Tipo.NO_ENVIADO, "NO_ENVIADO", motivo, null, null);
    }

    public static ResultadoProveedor incierto(String codigo, String motivo) {
        return new ResultadoProveedor(Tipo.INCIERTO, codigo, motivo, null, null);
    }
}
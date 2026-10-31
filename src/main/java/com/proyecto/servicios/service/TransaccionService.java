package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transaccion.HistorialTransaccionesRequestDTO;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.model.transaccion.TransaccionHistorialDTO;
import com.proyecto.servicios.model.transaccion.TransaccionResponseDTO;

import java.util.List;

public interface TransaccionService {

    /**
     * Ejecuta un pago de servicio con GestoPago.
     *
     * @param correoAutenticado identidad tomada del JWT validado; nunca del cuerpo de la peticion
     * @return resultado EXITOSA, o PENDIENTE_CONCILIACION si el proveedor no respondio de forma concluyente
     */
    TransaccionResponseDTO pagarServicio(PagarServicioRequestDTO request, String correoAutenticado);

    List<TransaccionHistorialDTO> obtenerHistorial(HistorialTransaccionesRequestDTO request, String correoAutenticado);
}
package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transaccion.TicketResponseDTO;

public interface EmailService {
    void enviarCodigoOtp(String destinatario, String codigoOtp);
    void enviarTicketComprobante(String destinatario, TicketResponseDTO ticket);
}

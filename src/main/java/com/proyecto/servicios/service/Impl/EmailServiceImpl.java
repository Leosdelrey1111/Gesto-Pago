package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String remitente;

    @Async
    @Override
    public void enviarCodigoOtp(String destinatario, String codigoOtp) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject("GestoPago Bank - Código de Seguridad 2FA");
            mensaje.setText(
                    "Estimado cliente,\n\n" +
                    "Su código de verificación para acceso seguro a GestoPago Bank es:\n\n" +
                    "   >>>  " + codigoOtp + "  <<<\n\n" +
                    "Este código tiene una vigencia de 3 minutos y es de un solo uso.\n" +
                    "Por motivos de seguridad, nunca comparta este código con nadie.\n\n" +
                    "Atentamente,\n" +
                    "Equipo de Seguridad Transaccional\n" +
                    "GestoPago Bank"
            );

            mailSender.send(mensaje);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {}", destinatario);
        } catch (Exception e) {
            log.error("[EMAIL 2FA ERROR] No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
        }
    }
}

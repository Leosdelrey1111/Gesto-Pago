package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.username:}")
    private String remitente;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Async
    @Override
    public void enviarCodigoOtp(String destinatario, String codigoOtp) {
        String asunto = "GestoPago Bank - Código de Seguridad 2FA";
        String contenido =
                "Estimado cliente,\n\n" +
                "Su código de verificación para acceso seguro a GestoPago Bank es:\n\n" +
                "   >>>  " + codigoOtp + "  <<<\n\n" +
                "Este código tiene una vigencia de 3 minutos y es de un solo uso.\n" +
                "Por motivos de seguridad, nunca comparta este código con nadie.\n\n" +
                "Atentamente,\n" +
                "Equipo de Seguridad Transaccional\n" +
                "GestoPago Bank";

        // Prioridad 1: Resend vía HTTPS REST (Puerto 443 - garantizado en Render/Cloud)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            if (enviarConResend(destinatario, asunto, contenido)) {
                return;
            }
        }

        // Prioridad 2: Brevo (Sendinblue) vía HTTPS REST (Puerto 443)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            if (enviarConBrevo(destinatario, asunto, contenido)) {
                return;
            }
        }

        // Prioridad 3: JavaMailSender convencional (SMTP 587 - local o VPS)
        enviarConSmtp(destinatario, asunto, contenido);
    }

    private boolean enviarConResend(String destinatario, String asunto, String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

            String sender = (remitente != null && remitente.contains("@") && !remitente.endsWith("@gmail.com"))
                    ? remitente
                    : "GestoPago Bank <onboarding@resend.dev>";

            Map<String, Object> body = new HashMap<>();
            body.put("from", sender);
            body.put("to", Collections.singletonList(destinatario));
            body.put("subject", asunto);
            body.put("text", contenido);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {} vía Resend HTTPS API", destinatario);
            return true;
        } catch (Exception e) {
            log.warn("[EMAIL 2FA RESEND ERROR] No se pudo enviar vía Resend: {}. Intentando siguiente método...", e.getMessage());
            return false;
        }
    }

    private boolean enviarConBrevo(String destinatario, String asunto, String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey.trim());

            String senderEmail = (remitente != null && !remitente.trim().isEmpty()) ? remitente.trim() : "soporte@gestopago.com";

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "GestoPago Bank", "email", senderEmail));
            body.put("to", List.of(Map.of("email", destinatario)));
            body.put("subject", asunto);
            body.put("textContent", contenido);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {} vía Brevo HTTPS API", destinatario);
            return true;
        } catch (Exception e) {
            log.warn("[EMAIL 2FA BREVO ERROR] No se pudo enviar vía Brevo: {}. Intentando fallback...", e.getMessage());
            return false;
        }
    }

    private void enviarConSmtp(String destinatario, String asunto, String contenido) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.trim().isEmpty()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(contenido);

            mailSender.send(mensaje);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {} vía SMTP convencional", destinatario);
        } catch (Exception e) {
            log.error("[EMAIL 2FA SMTP ERROR] Error enviando a {} vía SMTP (Nota: En Render Free, los puertos SMTP 25/465/587 son bloqueados. Configure RESEND_API_KEY en variables de entorno de Render): {}",
                    destinatario, e.getMessage());
        }
    }
}

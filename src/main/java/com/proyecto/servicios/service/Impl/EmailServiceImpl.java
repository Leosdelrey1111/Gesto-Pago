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

    @Value("${app.email.resend-api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${app.email.brevo-api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Async
    @Override
    public void enviarCodigoOtp(String destinatario, String codigoOtp) {
        String asunto = "GestoPago Bank - Código de Seguridad 2FA";
        String contenidoPlano =
                "Estimado cliente,\n\n" +
                "Su código de verificación para acceso seguro a GestoPago Bank es:\n\n" +
                "   >>>  " + codigoOtp + "  <<<\n\n" +
                "Este código tiene una vigencia de 3 minutos y es de un solo uso.\n" +
                "Por motivos de seguridad, nunca comparta este código con nadie.\n\n" +
                "Atentamente,\n" +
                "Equipo de Seguridad Transaccional\n" +
                "GestoPago Bank";

        String contenidoHtml = construirHtmlOtp(codigoOtp);

        // Prioridad 1: Resend vía HTTPS REST (Puerto 443 - garantizado en Render/Cloud)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            if (enviarConResend(destinatario, asunto, contenidoPlano, contenidoHtml)) {
                return;
            }
        }

        // Prioridad 2: Brevo (Sendinblue) vía HTTPS REST (Puerto 443)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            if (enviarConBrevo(destinatario, asunto, contenidoPlano, contenidoHtml)) {
                return;
            }
        }

        // Prioridad 3: JavaMailSender convencional (SMTP 587 - local o VPS)
        enviarConSmtp(destinatario, asunto, contenidoPlano, contenidoHtml);
    }

    @Override
    public void enviarTicketComprobante(String destinatario, com.proyecto.servicios.model.transaccion.TicketResponseDTO ticket) {
        log.info("[EMAIL COMPROBANTE] Iniciando envío de ticket folio #{} hacia {}", ticket != null ? ticket.getFolio() : "null", destinatario);
        if (destinatario == null || destinatario.trim().isEmpty() || ticket == null) {
            log.warn("[EMAIL COMPROBANTE] Destinatario o ticket nulos. No se puede enviar.");
            return;
        }

        boolean esExitosa = "PAGADO / APROBADO".equalsIgnoreCase(ticket.getEstatus()) || "EXITOSA".equalsIgnoreCase(ticket.getEstatus());
        String asunto = (esExitosa ? "[APROBADO] Comprobante de Pago" : "[RECHAZADO] Comprobante de Pago")
                + " - Folio #" + ticket.getFolio() + " | GestoPago Bank";

        String contenidoPlano =
                "Estimado cliente,\n\n" +
                (esExitosa ? "Su pago ha sido procesado exitosamente." : "Su intento de pago fue rechazado por el proveedor. No se aplicó ningún cargo.") + "\n\n" +
                "Detalles de la Operación:\n" +
                "--------------------------------------------------\n" +
                "Folio: #" + ticket.getFolio() + "\n" +
                "Estatus: " + ticket.getEstatus() + "\n" +
                "Servicio: " + ticket.getServicio() + "\n" +
                "Producto: " + ticket.getProducto() + "\n" +
                "Referencia / Teléfono: " + ticket.getReferenciaOTelefono() + "\n" +
                "Monto: $" + (ticket.getMonto() != null ? ticket.getMonto().toPlainString() : "0.00") + " MXN\n" +
                "UPC: " + (ticket.getUpc() != null ? ticket.getUpc() : "N/A") + "\n" +
                "Autorización: " + (ticket.getNumeroAutorizacion() != null ? ticket.getNumeroAutorizacion() : "N/A") + "\n" +
                "Fecha / Hora: " + (ticket.getFechaOperacion() != null ? ticket.getFechaOperacion().toString() : "N/A") + "\n" +
                "Cuenta Origen: " + ticket.getNumeroCuenta() + "\n" +
                "--------------------------------------------------\n\n" +
                (ticket.getLeyenda() != null ? ticket.getLeyenda() + "\n\n" : "") +
                "Para cualquier duda o aclaración: 800-437-8672\n\n" +
                "Atentamente,\n" +
                "GestoPago Bank";

        String contenidoHtml = construirHtmlTicket(ticket, esExitosa);

        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            if (enviarConResend(destinatario, asunto, contenidoPlano, contenidoHtml)) {
                return;
            }
        }

        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            if (enviarConBrevo(destinatario, asunto, contenidoPlano, contenidoHtml)) {
                return;
            }
        }

        enviarConSmtp(destinatario, asunto, contenidoPlano, contenidoHtml);
    }

    private String construirHtmlTicket(com.proyecto.servicios.model.transaccion.TicketResponseDTO ticket, boolean esExitosa) {
        String colorStatus = esExitosa ? "#10B981" : "#EF4444";
        String bgBadge = esExitosa ? "rgba(16, 185, 129, 0.12)" : "rgba(239, 68, 68, 0.12)";
        String borderBadge = esExitosa ? "#10B981" : "#EF4444";
        String icono = esExitosa ? "✓" : "✕";
        String tituloBadge = esExitosa ? "PAGO APROBADO" : "PAGO RECHAZADO";

        String montoStr = ticket.getMonto() != null ? "$" + ticket.getMonto().toPlainString() + " MXN" : "$0.00 MXN";
        String fechaStr = ticket.getFechaOperacion() != null ? ticket.getFechaOperacion().toString().replace('T', ' ') : "N/A";
        String upcStr = ticket.getUpc() != null ? ticket.getUpc() : "N/A";
        String autStr = ticket.getNumeroAutorizacion() != null ? ticket.getNumeroAutorizacion() : "N/A";
        String idTxStr = ticket.getIdTransaccionProveedor() != null ? ticket.getIdTransaccionProveedor() : "N/A";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Comprobante de Pago - GestoPago Bank</title>
              <style>
                body { margin: 0; padding: 0; background-color: #070B14; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #F8FAFC; }
                .wrapper { width: 100%; table-layout: fixed; background-color: #070B14; padding: 36px 0; }
                .container { max-width: 540px; margin: 0 auto; background: #0E1626; border-radius: 20px; border: 1px solid rgba(56, 189, 248, 0.2); overflow: hidden; box-shadow: 0 20px 45px rgba(0,0,0,0.7); }
                .header { padding: 28px 32px 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.06); background: linear-gradient(180deg, #131E35 0%%, #0E1626 100%%); }
                .bank-logo { font-size: 20px; font-weight: 800; letter-spacing: 2px; color: #38BDF8; margin: 0; text-transform: uppercase; }
                .bank-sub { font-size: 10px; color: #94A3B8; margin-top: 4px; letter-spacing: 1px; text-transform: uppercase; }
                .content { padding: 32px 32px; }
                .status-badge { display: inline-block; padding: 8px 18px; border-radius: 9999px; font-size: 12px; font-weight: 800; letter-spacing: 1px; margin-bottom: 20px; border: 1px solid %s; background-color: %s; color: %s; }
                .amount-box { text-align: center; margin-bottom: 28px; padding: 20px; background: rgba(15, 23, 42, 0.6); border-radius: 16px; border: 1px solid rgba(255,255,255,0.05); }
                .amount-label { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #94A3B8; margin-bottom: 6px; }
                .amount-val { font-size: 32px; font-weight: 800; color: #FFFFFF; letter-spacing: -0.5px; }
                .ticket-table { width: 100%%; border-collapse: collapse; margin-bottom: 24px; }
                .ticket-table td { padding: 10px 0; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.05); }
                .col-label { color: #94A3B8; text-align: left; }
                .col-val { color: #F1F5F9; font-weight: 600; text-align: right; font-family: -apple-system, BlinkMacSystemFont, monospace; }
                .notice-box { background: rgba(56, 189, 248, 0.05); border-left: 3px solid #0284C7; padding: 12px 16px; border-radius: 8px; margin-bottom: 24px; }
                .notice-text { font-size: 11.5px; color: #94A3B8; margin: 0; line-height: 1.5; }
                .footer { padding: 22px 32px; background-color: #090E1A; text-align: center; border-top: 1px solid rgba(255,255,255,0.04); }
                .footer-text { font-size: 11px; color: #64748B; margin: 0; line-height: 1.6; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="container">
                  <div class="header">
                    <div class="bank-logo">GestoPago Bank</div>
                    <div class="bank-sub">Ecosistema Financiero Digital</div>
                  </div>
                  <div class="content" style="text-align: center;">
                    <div class="status-badge">%s %s</div>
                    <div class="amount-box">
                      <div class="amount-label">Monto de la Transacción</div>
                      <div class="amount-val" style="color: %s;">%s</div>
                    </div>
                    <table class="ticket-table">
                      <tr><td class="col-label">Folio de Control</td><td class="col-val">#%d</td></tr>
                      <tr><td class="col-label">Servicio</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">Producto</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">Referencia / Teléfono</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">Código UPC</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">No. Autorización</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">ID Transacción Proveedor</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">Cuenta Bancaria</td><td class="col-val">%s</td></tr>
                      <tr><td class="col-label">Fecha y Hora</td><td class="col-val">%s</td></tr>
                    </table>
                    <div class="notice-box">
                      <p class="notice-text">%s</p>
                    </div>
                  </div>
                  <div class="footer">
                    <p class="footer-text">
                      Para cualquier duda o aclaración, comuníquese al <strong>800-437-8672</strong>.<br>
                      Comprobante emitido automáticamente por GestoPago Bank. Conserve este comprobante.
                    </p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                borderBadge, bgBadge, colorStatus,
                icono, tituloBadge,
                colorStatus, montoStr,
                ticket.getFolio() != null ? ticket.getFolio() : 0,
                ticket.getServicio() != null ? ticket.getServicio() : "N/A",
                ticket.getProducto() != null ? ticket.getProducto() : "N/A",
                ticket.getReferenciaOTelefono() != null ? ticket.getReferenciaOTelefono() : "N/A",
                upcStr,
                autStr,
                idTxStr,
                ticket.getNumeroCuenta() != null ? ticket.getNumeroCuenta() : "N/A",
                fechaStr,
                ticket.getLeyenda() != null ? ticket.getLeyenda() : "Conserve este comprobante para cualquier aclaración."
            );
    }

    private String construirHtmlOtp(String codigoOtp) {
        String plantilla = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Codigo de Verificacion - GestoPago Bank</title>
              <style>
                body { margin: 0; padding: 0; background-color: #0B0F19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #F8FAFC; }
                .wrapper { width: 100%; table-layout: fixed; background-color: #0B0F19; padding: 40px 0; }
                .container { max-width: 520px; margin: 0 auto; background: #0F172A; border-radius: 20px; border: 1px solid rgba(56, 189, 248, 0.2); overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
                .header { padding: 32px 32px 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.06); }
                .logo-title { font-size: 20px; font-weight: 800; letter-spacing: 1.5px; color: #38BDF8; margin: 0; text-transform: uppercase; }
                .subtitle { font-size: 11px; color: #94A3B8; margin-top: 4px; letter-spacing: 0.5px; text-transform: uppercase; }
                .content { padding: 36px 32px; text-align: center; }
                .heading { font-size: 18px; font-weight: 700; color: #FFFFFF; margin: 0 0 12px; }
                .paragraph { font-size: 13.5px; line-height: 1.6; color: #94A3B8; margin: 0 0 28px; }
                .otp-box { background: rgba(56, 189, 248, 0.08); border: 2px dashed #0284C7; border-radius: 14px; padding: 18px 24px; margin: 0 auto 28px; display: inline-block; }
                .otp-code { font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 36px; font-weight: 800; letter-spacing: 12px; color: #38BDF8; margin-left: 12px; }
                .warning-box { background: rgba(245, 158, 11, 0.1); border-left: 3px solid #F59E0B; padding: 12px 16px; border-radius: 8px; text-align: left; margin-bottom: 24px; }
                .warning-text { font-size: 12px; color: #FCD34D; margin: 0; line-height: 1.5; }
                .footer { padding: 24px 32px; background-color: #0A0E1A; text-align: center; border-top: 1px solid rgba(255,255,255,0.04); }
                .footer-text { font-size: 11px; color: #64748B; margin: 0; line-height: 1.6; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="container">
                  <div class="header">
                    <h1 class="logo-title">GESTOPAGO BANK</h1>
                    <div class="subtitle">Portal Transaccional Corporativo - Cifrado AES-256</div>
                  </div>
                  <div class="content">
                    <h2 class="heading">Codigo de Seguridad 2FA</h2>
                    <p class="paragraph">Utiliza el siguiente codigo unico de verificacion para autenticar tu acceso al portal bancario:</p>
                    <div class="otp-box">
                      <div class="otp-code">{{CODIGO_OTP}}</div>
                    </div>
                    <div class="warning-box">
                      <p class="warning-text"><strong>Seguridad Bancaria:</strong> Este codigo expira en <strong>3 minutos</strong> y es de un solo uso. Nuestro personal jamas te solicitara este codigo por telefono o mensaje.</p>
                    </div>
                  </div>
                  <div class="footer">
                    <p class="footer-text">Este es un mensaje automatico de seguridad. Si no intentaste iniciar sesion, cambia tu contrasena de inmediato.<br>&copy; 2026 GestoPago Bank Inc. Todos los derechos reservados.</p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """;
        return plantilla.replace("{{CODIGO_OTP}}", codigoOtp);
    }

    private boolean enviarConResend(String destinatario, String asunto, String contenidoPlano, String contenidoHtml) {
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
            body.put("text", contenidoPlano);
            body.put("html", contenidoHtml);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {} vía Resend HTTPS API", destinatario);
            return true;
        } catch (Exception e) {
            log.warn("[EMAIL 2FA RESEND ERROR] No se pudo enviar vía Resend: {}. Intentando siguiente método...", e.getMessage());
            return false;
        }
    }

    private boolean enviarConBrevo(String destinatario, String asunto, String contenidoPlano, String contenidoHtml) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey.trim());

            String senderEmail = (remitente != null && !remitente.trim().isEmpty()) ? remitente.trim() : "bhleonel705@gmail.com";

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "GestoPago Bank", "email", senderEmail));
            body.put("to", List.of(Map.of("email", destinatario)));
            body.put("subject", asunto);
            body.put("textContent", contenidoPlano);
            body.put("htmlContent", contenidoHtml);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            log.info("[EMAIL TICKET/2FA] Correo ('{}') enviado exitosamente a {} vía Brevo HTTPS API", asunto, destinatario);
            return true;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.warn("[EMAIL BREVO ERROR] Rechazado por Brevo HTTP {}: {}. Asunto='{}'. Intentando fallback...", e.getStatusCode(), e.getResponseBodyAsString(), asunto);
            return false;
        } catch (Exception e) {
            log.warn("[EMAIL BREVO ERROR] Error inesperado vía Brevo para asunto='{}': {}. Intentando fallback...", asunto, e.getMessage());
            return false;
        }
    }

    private void enviarConSmtp(String destinatario, String asunto, String contenidoPlano, String contenidoHtml) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                    new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            if (remitente != null && !remitente.trim().isEmpty()) {
                helper.setFrom(remitente, "GestoPago Bank");
            }
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoPlano, contenidoHtml);

            mailSender.send(mimeMessage);
            log.info("[EMAIL 2FA] Código OTP enviado exitosamente a {} vía SMTP convencional", destinatario);
        } catch (Exception e) {
            log.error("[EMAIL 2FA SMTP ERROR] Error enviando a {} vía SMTP: {}", destinatario, e.getMessage());
        }
    }
}

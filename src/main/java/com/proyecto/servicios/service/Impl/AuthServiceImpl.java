package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_EXPIRACION_MINUTOS = 3;
    private static final int OTP_ENFRIAMIENTO_SEGUNDOS = 60;
    private final UsuarioRepository usuarioRepository;
    private final com.proyecto.servicios.service.EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String solicitarOtp(Usuario usuario) {
        // Enfriamiento: evita usar el endpoint para inundar de correos a un usuario.
        if (usuario.getOtpExpiracion() != null) {
            OffsetDateTime emitido = usuario.getOtpExpiracion().minusMinutes(OTP_EXPIRACION_MINUTOS);
            if (OffsetDateTime.now().isBefore(emitido.plusSeconds(OTP_ENFRIAMIENTO_SEGUNDOS))) {
                throw new com.proyecto.servicios.exception.ReglaNegocioException(
                        "Espere un momento antes de solicitar un nuevo código.");
            }
        }

        int codigo = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(codigo);

        usuario.setOtpCode(otp);
        usuario.setOtpExpiracion(OffsetDateTime.now().plusMinutes(OTP_EXPIRACION_MINUTOS));
        usuarioRepository.save(usuario);

        log.info("[2FA OTP] Código de verificación generado para {}", usuario.getCorreo());

        // Envío asíncrono del correo mediante JavaMailSender
        try {
            emailService.enviarCodigoOtp(usuario.getCorreo(), otp);
        } catch (Exception e) {
            log.error("Error al disparar el envío de correo OTP: {}", e.getMessage());
        }

        return otp;
    }

    @Override
    @Transactional
    public boolean validarOtp(Usuario usuario, String otp) {
        if (usuario.getOtpCode() == null || usuario.getOtpExpiracion() == null) {
            return false;
        }

        if (OffsetDateTime.now().isAfter(usuario.getOtpExpiracion())) {
            limpiarOtp(usuario);
            return false;
        }

        boolean coincide = usuario.getOtpCode().equals(otp.trim());
        if (coincide) {
            limpiarOtp(usuario);
        }
        return coincide;
    }

    @Override
    @Transactional
    public void registrarNuevaSesion(Usuario usuario, String sessionId, String deviceId) {
        usuario.setSessionId(sessionId);
        usuario.setSessionDeviceId(deviceId);
        usuarioRepository.save(usuario);
        log.info("[SESION UNICA] Nueva sesión registrada para {} en dispositivo {}. SesionId: {}", 
                usuario.getCorreo(), deviceId, sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public void validarSesionActiva(String correo, String sessionId, String deviceId) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UnauthorizedAccessException("El usuario se encuentra inactivo");
        }

        // Si el token tiene sessionId pero ya no coincide con el de la BD, fue expulsado
        if (sessionId != null && usuario.getSessionId() != null && !usuario.getSessionId().equals(sessionId)) {
            log.warn("[SESION EXPULSADA] Sesión {} revocada para {} porque se inició en otro dispositivo", sessionId, correo);
            throw new UnauthorizedAccessException("Tu sesión ha sido cerrada porque se inició sesión en otro dispositivo");
        }

        // Si se envió X-Device-Id y difiere del registrado como sesión activa
        if (deviceId != null && usuario.getSessionDeviceId() != null && !usuario.getSessionDeviceId().equals(deviceId)) {
            log.warn("[DISPOSITIVO NO COINCIDE] Device {} difiere del activo {} para {}", deviceId, usuario.getSessionDeviceId(), correo);
            throw new UnauthorizedAccessException("Dispositivo no autorizado o sesión activa en otro dispositivo");
        }
    }

    @Override
    @Transactional
    public void actualizarPinTransaccional(String correo, String nuevoPin) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));
        // Cifrado BCrypt del PIN de 4 dígitos
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        usuario.setPinTransaccional(encoder.encode(nuevoPin.trim()));
        usuarioRepository.save(usuario);
        log.info("[PIN BANCARIO] PIN transaccional actualizado exitosamente para {}", correo);
    }

    private void limpiarOtp(Usuario usuario) {
        usuario.setOtpCode(null);
        usuario.setOtpExpiracion(null);
        usuarioRepository.save(usuario);
    }
}


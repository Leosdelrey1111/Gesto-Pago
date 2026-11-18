package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de verificación 2FA por usuario para impedir fuerza bruta sobre el código de 6 dígitos.
 * Tras {@value #MAX_INTENTOS} fallos el usuario queda bloqueado {@code BLOQUEO} minutos.
 */
@Component
public class OtpAttemptLimiter {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(5);

    private record Estado(int fallos, Instant desde) { }

    private final ConcurrentHashMap<String, Estado> intentos = new ConcurrentHashMap<>();

    public void verificarPermitido(String correo) {
        Estado e = intentos.get(clave(correo));
        if (e == null) {
            return;
        }
        if (Instant.now().isAfter(e.desde().plus(BLOQUEO))) {
            intentos.remove(clave(correo));
            return;
        }
        if (e.fallos() >= MAX_INTENTOS) {
            throw new UnauthorizedAccessException("Demasiados intentos. Intente de nuevo en unos minutos.");
        }
    }

    public void registrarFallo(String correo) {
        intentos.merge(clave(correo), new Estado(1, Instant.now()),
                (actual, nuevo) -> new Estado(actual.fallos() + 1, actual.desde()));
    }

    public void reiniciar(String correo) {
        intentos.remove(clave(correo));
    }

    private String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}

package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica quien es el cliente y que puede operar la cuenta, ANTES de tomar bloqueos o llamar al proveedor.
 * La identidad sale del JWT validado por el filtro de seguridad, nunca del cuerpo de la peticion.
 */
@lombok.extern.slf4j.Slf4j
@Component
public class AutorizacionClienteValidator {

    static final String MSG_SIN_PERMISO = "No tiene permiso para operar esta cuenta";

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    public AutorizacionClienteValidator(UsuarioRepository usuarioRepository,
                                        CuentaRepository cuentaRepository,
                                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida que el usuario autenticado este habilitado, sea el titular de la cuenta y, si se indica un PIN,
     * que lo conozca (reautenticacion por operacion financiera).
     *
     * @param passwordOPin PIN/password de autorizacion; {@code null} omite esa verificacion (consultas).
     */
    @Transactional(readOnly = true)
    public Cuenta autorizar(String correoAutenticado, String numeroCuenta, String passwordOPin) {
        if (correoAutenticado == null || correoAutenticado.isBlank()) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }

        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new AccesoDenegadoException(MSG_SIN_PERMISO));
        Cliente cliente = usuario.getCliente();
        if (!Boolean.TRUE.equals(usuario.getActivo()) || cliente == null
                || !Boolean.TRUE.equals(cliente.getActivo()) || Boolean.TRUE.equals(cliente.getBloqueado())) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }

        // Cuenta inexistente y cuenta ajena producen la misma respuesta: no se revela cuales cuentas existen.
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .filter(c -> c.getCliente() != null && cliente.getId().equals(c.getCliente().getId()))
                .orElseThrow(() -> new AccesoDenegadoException(MSG_SIN_PERMISO));

        // Blindaje contra operaciones en cuentas bloqueadas o canceladas
        if (cuenta.getEstatus() != com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta.ACTIVA) {
            throw new com.proyecto.servicios.exception.BusinessValidationException(
                "La cuenta se encuentra " + cuenta.getEstatus() + " y no puede realizar transacciones financieras."
            );
        }

        if (passwordOPin != null && !passwordOPin.isBlank()) {
            boolean coincidePin = false;
            if (usuario.getPinTransaccional() != null && !usuario.getPinTransaccional().isBlank()) {
                coincidePin = passwordEncoder.matches(passwordOPin, usuario.getPinTransaccional());
            }
            boolean coincidePassword = passwordEncoder.matches(passwordOPin, usuario.getPassword());

            if (!coincidePin && !coincidePassword) {
                log.warn("[SEGURIDAD PIN] Intento de pago rechazado por PIN o contraseña incorrectos para usuario={}", correoAutenticado);
                throw new UnauthorizedAccessException("El password o PIN de autorización es incorrecto");
            }
        } else {
            throw new UnauthorizedAccessException("El PIN transaccional es obligatorio");
        }
        return cuenta;
    }
}
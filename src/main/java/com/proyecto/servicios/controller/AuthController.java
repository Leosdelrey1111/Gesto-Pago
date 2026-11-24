package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.security.JwtUtil;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.model.security.AuthRequest;
import com.proyecto.servicios.model.security.AuthResponse;
import com.proyecto.servicios.model.security.ValidarOtpRequest;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String HEADER_PRE_AUTH = "X-Pre-Auth-Token";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final com.proyecto.servicios.service.OAuthClientService oAuthClientService;
    private final com.proyecto.servicios.security.OtpAttemptLimiter otpAttemptLimiter;

    private void exigirPreAuth(String preAuthToken, String correo) {
        if (!jwtUtil.isValidPreAuthToken(preAuthToken, correo)) {
            throw new UnauthorizedAccessException("Sesión de verificación inválida. Inicie sesión nuevamente.");
        }
    }

    @PostMapping("/oauth/token")
    public ResponseEntity<com.proyecto.servicios.model.security.OAuthTokenResponse> obtenerTokenOAuth(
            @Valid @RequestBody com.proyecto.servicios.model.security.OAuthTokenRequest request) {
        return ResponseEntity.ok(oAuthClientService.emitirToken(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest authRequest,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        String deviceIdFinal = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : UUID.randomUUID().toString();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getCorreo(), authRequest.getPassword())
            );
        } catch (DisabledException e) {
            throw new UnauthorizedAccessException("El usuario está inactivo.");
        } catch (AuthenticationException e) {
            throw new UnauthorizedAccessException("Credenciales inválidas");
        }

        Usuario usuario = usuarioRepository.findByCorreo(authRequest.getCorreo())
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        // Asegurar que el usuario tenga clave secreta TOTP para Google Authenticator
        boolean primerRegistro = false;
        if (usuario.getTotpSecret() == null || usuario.getTotpSecret().isBlank()) {
            usuario.setTotpSecret(com.proyecto.servicios.util.GoogleAuthenticatorUtil.generarClaveSecreta());
            usuario.setTotpActivado(false);
            usuarioRepository.save(usuario);
            primerRegistro = true;
        } else if (!Boolean.TRUE.equals(usuario.getTotpActivado())) {
            primerRegistro = true;
        }

        // El código por correo NO se envía aquí: solo cuando el usuario elige ese método (/auth/2fa/reenviar-otp).

        // La semilla TOTP y el QR solo se entregan durante el primer enrolamiento (o tras un reset autorizado por ADMIN).
        String otpAuthUrl = primerRegistro
                ? com.proyecto.servicios.util.GoogleAuthenticatorUtil.generarOtpAuthUrl(
                        usuario.getCorreo(), usuario.getTotpSecret(), "GestoPago Bank")
                : null;
        String totpSecretVisible = primerRegistro ? usuario.getTotpSecret() : null;

        // Pre-Auth Token (Temporal de 5 min para completar el segundo factor)
        String preAuthToken = jwtUtil.generatePreAuthToken(usuario.getCorreo(), deviceIdFinal);

        // Enmascarar teléfono y correo para visualización bancaria segura
        String telefonoEnmascarado = "•••• 5678";
        try {
            if (usuario.getCliente() != null && usuario.getCliente().getTelefonoMovil() != null) {
                String tel = String.valueOf(usuario.getCliente().getTelefonoMovil());
                telefonoEnmascarado = (tel.length() >= 4) ? "•••• •••• " + tel.substring(tel.length() - 4) : "•••• " + tel;
            }
        } catch (Exception ex) {
            log.warn("No se pudo obtener el teléfono del cliente: {}", ex.getMessage());
        }

        String correo = usuario.getCorreo();
        int atIdx = correo != null ? correo.indexOf('@') : -1;
        String correoEnmascarado = (correo != null && atIdx > 2)
                ? correo.substring(0, 2) + "•••••" + correo.substring(atIdx)
                : (correo != null ? "•••••" + correo.substring(Math.max(0, atIdx)) : "•••••");

        AuthResponse response = AuthResponse.builder()
                .requiere2FA(true)
                .preAuthToken(preAuthToken)
                .mensaje("Verificación de Identidad requerida.")
                .otpAuthUrl(otpAuthUrl)
                .totpSecret(totpSecretVisible)
                .primerRegistro2FA(primerRegistro)
                .telefonoEnmascarado(telefonoEnmascarado)
                .correoEnmascarado(correoEnmascarado)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/validar")
    public ResponseEntity<AuthResponse> validar2FA(
            @Valid @RequestBody ValidarOtpRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = HEADER_PRE_AUTH, required = false) String preAuthToken) {

        // Sin haber pasado el primer factor (contraseña) no se puede intentar el segundo.
        exigirPreAuth(preAuthToken, request.getCorreo());
        otpAttemptLimiter.verificarPermitido(request.getCorreo());

        String deviceIdFinal = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "DEFAULT_DEVICE";

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        boolean esValidoOtp = authService.validarOtp(usuario, request.getOtp());
        boolean esValidoTotp = usuario.getTotpSecret() != null && 
                com.proyecto.servicios.util.GoogleAuthenticatorUtil.validarCodigo(usuario.getTotpSecret(), request.getOtp());

        if (!esValidoOtp && !esValidoTotp) {
            otpAttemptLimiter.registrarFallo(request.getCorreo());
            throw new UnauthorizedAccessException("Código de verificación incorrecto o expirado");
        }
        otpAttemptLimiter.reiniciar(request.getCorreo());

        if (esValidoTotp && !Boolean.TRUE.equals(usuario.getTotpActivado())) {
            usuario.setTotpActivado(true);
            usuarioRepository.save(usuario);
        }

        // Registrar la nueva sesión única (expulsando cualquier sesión anterior)
        String nuevoSessionId = UUID.randomUUID().toString();
        authService.registrarNuevaSesion(usuario, nuevoSessionId, deviceIdFinal);

        // Emitir el JWT de sesión definitivo con claim de sessionId, deviceId y rol
        String jwt = jwtUtil.generateToken(usuario.getCorreo(), nuevoSessionId, deviceIdFinal, usuario.getRol());

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .requiere2FA(false)
                .mensaje("Autenticación exitosa")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/reenviar-otp")
    public ResponseEntity<?> reenviarOtp(
            @Valid @RequestBody com.proyecto.servicios.model.security.ReenviarOtpRequest request,
            @RequestHeader(value = HEADER_PRE_AUTH, required = false) String preAuthToken) {

        exigirPreAuth(preAuthToken, request.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        authService.solicitarOtp(usuario);

        String correo = usuario.getCorreo();
        int atIdx = correo != null ? correo.indexOf('@') : -1;
        String correoEnmascarado = (correo != null && atIdx > 2)
                ? correo.substring(0, 2) + "•••••" + correo.substring(atIdx)
                : (correo != null ? "•••••" + correo.substring(Math.max(0, atIdx)) : "•••••");

        return ResponseEntity.ok(java.util.Map.of(
                "mensaje", "Código de seguridad enviado a " + correoEnmascarado,
                "correoEnmascarado", correoEnmascarado,
                "status", 200
        ));
    }

    /** Solo un administrador puede autorizar la re-vinculación de Google Authenticator si el cliente cambió de teléfono. */
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reset-2fa")
    public ResponseEntity<?> resetear2FA(@Valid @RequestBody com.proyecto.servicios.model.security.ReenviarOtpRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        usuario.setTotpSecret(com.proyecto.servicios.util.GoogleAuthenticatorUtil.generarClaveSecreta());
        usuario.setTotpActivado(false);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(java.util.Map.of(
                "mensaje", "Token 2FA restablecido. El usuario deberá vincular su nuevo dispositivo en el próximo acceso.",
                "correo", usuario.getCorreo(),
                "status", 200
        ));
    }

    @PostMapping("/pin")
    public ResponseEntity<?> actualizarPin(
            @Valid @RequestBody com.proyecto.servicios.model.security.ActualizarPinRequest request,
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedAccessException("Sesión no autenticada");
        }

        authService.actualizarPinTransaccional(authentication.getName(), request.getPin());

        return ResponseEntity.ok(java.util.Map.of(
                "mensaje", "PIN de seguridad configurado exitosamente",
                "status", 200
        ));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarNuevoUsuario(
            @Valid @RequestBody com.proyecto.servicios.model.security.RegistroRapidoRequest req) {

        if (usuarioRepository.existsByCorreo(req.getCorreo().trim().toLowerCase())) {
            throw new com.proyecto.servicios.exception.UnauthorizedAccessException("El correo electrónico ya se encuentra registrado");
        }

        com.proyecto.servicios.repository.onboarding.ClienteRepository clienteRepo = 
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getRequiredWebApplicationContext(
                                ((org.springframework.web.context.request.ServletRequestAttributes) 
                                        org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                                        .getRequest().getServletContext()
                        ).getBean(com.proyecto.servicios.repository.onboarding.ClienteRepository.class);

        String curpUpper = req.getCurp().trim().toUpperCase();
        String rfcUpper = req.getRfc().trim().toUpperCase();

        if (clienteRepo.existsByCurp(curpUpper)) {
            throw new com.proyecto.servicios.exception.DuplicateResourceException("Ya existe un cliente registrado con la CURP proporcionada");
        }
        if (clienteRepo.existsByRfc(rfcUpper)) {
            throw new com.proyecto.servicios.exception.DuplicateResourceException("Ya existe un cliente registrado con el RFC proporcionado");
        }

        // Crear Cliente bancario con datos de identidad y fiscales reales
        com.proyecto.servicios.entity.onboarding.Cliente cliente = new com.proyecto.servicios.entity.onboarding.Cliente();
        cliente.setNombre(req.getNombre().trim());
        cliente.setSegundoNombre(req.getSegundoNombre() != null && !req.getSegundoNombre().trim().isEmpty() ? req.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(req.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(req.getApellidoMaterno().trim());
        cliente.setCorreoElectronico(req.getCorreo().trim().toLowerCase());
        cliente.setTelefonoMovil(Long.parseLong(req.getTelefono().trim()));
        cliente.setLadaMovil(52);
        cliente.setActivo(true);
        cliente.setCurp(curpUpper);
        cliente.setRfc(rfcUpper);
        cliente.setFechaNacimiento(req.getFechaNacimiento());
        cliente.setCodigoPostal(req.getCodigoPostal());
        cliente.setSexo(com.proyecto.servicios.entity.onboarding.catalogos.Sexo.MASCULINO);
        cliente.setNacionalidad(com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad.MEXICANA);
        cliente.setEstadoCivil(com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil.SOLTERO);
        cliente.setOcupacion("Titular de Cuenta Digital");
        cliente.setEmpresa("Independiente");
        cliente.setIngresoMensual(new java.math.BigDecimal("15000.00"));

        com.proyecto.servicios.service.CuentaService cuentaService = 
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getRequiredWebApplicationContext(
                                ((org.springframework.web.context.request.ServletRequestAttributes) 
                                        org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                                        .getRequest().getServletContext()
                        ).getBean(com.proyecto.servicios.service.CuentaService.class);

        com.proyecto.servicios.entity.onboarding.Cliente clienteGuardado = clienteRepo.save(cliente);

        // Crear cuenta bancaria con saldo inicial en $0.00 (los fondos se acreditan posteriormente)
        com.proyecto.servicios.entity.onboarding.Cuenta cuenta = cuentaService.crearCuentaParaCliente(clienteGuardado.getId());

        // Crear usuario sin PIN transaccional; lo configurará al iniciar sesión por primera vez
        org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        Usuario usuario = Usuario.builder()
                .correo(req.getCorreo().trim().toLowerCase())
                .password(encoder.encode(req.getPassword()))
                .cliente(clienteGuardado)
                .activo(true)
                .rol("CLIENTE")
                .pinTransaccional(null)
                .build();

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(java.util.Map.of(
                "mensaje", "Cuenta bancaria y usuario creados exitosamente",
                "numeroCuenta", cuenta.getNumeroCuenta(),
                "saldoInicial", cuenta.getSaldo(),
                "correo", usuario.getCorreo(),
                "status", 201
        ));
    }

    @GetMapping("/verificar-sesion")
    public ResponseEntity<?> verificarSesion(
            org.springframework.security.core.Authentication authentication,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedAccessException("Sesión no autenticada");
        }

        Usuario usuario = usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new UnauthorizedAccessException("Usuario no encontrado"));

        boolean tienePin = usuario.getPinTransaccional() != null && !usuario.getPinTransaccional().isBlank();

        return ResponseEntity.ok(java.util.Map.of(
                "correo", usuario.getCorreo(),
                "activo", usuario.getActivo(),
                "tienePin", tienePin,
                "mensaje", "Sesión activa y válida"
        ));
    }
}

package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.security.JwtUtil;
import com.proyecto.servicios.model.security.AuthRequest;
import com.proyecto.servicios.model.security.AuthResponse;
import com.proyecto.servicios.model.security.BiometricAuthRequest;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getCorreo(), authRequest.getPassword())
            );
        } catch (DisabledException e) {
            throw new UnauthorizedAccessException("El usuario está inactivo.");
        } catch (AuthenticationException e) {
            throw new UnauthorizedAccessException("Credenciales invalidas");
        }

        String jwt = jwtUtil.generateToken(authRequest.getCorreo());
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/login-biometrico")
    public ResponseEntity<AuthResponse> loginBiometrico(@Valid @RequestBody BiometricAuthRequest request) {
        // Aquí simulamos que si llega el token biométrico desde el Front (ej. FaceID validado en dispositivo),
        // validamos que exista y esté asociado al usuario. Para este caso generamos el JWT directamente.
        if (request.getBiometricToken() == null || request.getBiometricToken().isEmpty()) {
            throw new UnauthorizedAccessException("Token biométrico inválido");
        }

        // TODO: Validar en base de datos que el usuario con ese correo tiene habilitado biometría 
        // y coincide el token si se guarda del lado del back.

        String jwt = jwtUtil.generateToken(request.getCorreo());
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}

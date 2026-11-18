package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Usuario;

public interface AuthService {
    String solicitarOtp(Usuario usuario);
    boolean validarOtp(Usuario usuario, String otp);
    void registrarNuevaSesion(Usuario usuario, String sessionId, String deviceId);
    void validarSesionActiva(String correo, String sessionId, String deviceId);
    void actualizarPinTransaccional(String correo, String nuevoPin);
}


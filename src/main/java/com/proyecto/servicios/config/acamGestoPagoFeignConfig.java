package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Configuracion de los clientes Feign de GestoPago.
 * - El Bearer se agrega por peticion; si no existe, la llamada falla ANTES de salir a la red.
 * - Sin reintentos automaticos: reenviar una operacion financiera tras un timeout puede duplicar el cargo.
 */
public class acamGestoPagoFeignConfig {

    @Bean
    public RequestInterceptor gestoPagoAuthInterceptor(
            GestoPagoTokenService tokenService,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo) {
        return requestTemplate -> {
            String token = tokenService
                    .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .map(GestoPagoToken::getToken)
                    .orElseThrow(() -> new ProveedorNoDisponibleException(
                            "No hay token vigente para comunicarse con GestoPago"));
            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }

    @Bean
    public Retryer gestoPagoRetryer() {
        return Retryer.NEVER_RETRY;
    }
}

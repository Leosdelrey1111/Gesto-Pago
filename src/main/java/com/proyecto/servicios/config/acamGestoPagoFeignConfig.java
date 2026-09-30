package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class acamGestoPagoFeignConfig {

    @Bean
    public RequestInterceptor gestoPagoAuthInterceptor(
            GestoPagoTokenService tokenService,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo) {
        return requestTemplate -> tokenService
                .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .ifPresent(token -> requestTemplate.header("Authorization", "Bearer " + token));
    }
}
package com.proyecto.servicios.client;

import com.proyecto.servicios.config.acamGestoPagoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoTx",
        url = "${gestopago.auth.url}",
        configuration = acamGestoPagoFeignConfig.class)
public interface GestoPagoTxClient {

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<String> sendTx(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );
}

package com.proyecto.servicios.client;


import com.proyecto.servicios.config.acamGestoPagoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "gestoPagoCatProduct",
        url = "${gestopago.auth.url}",
        configuration = acamGestoPagoFeignConfig.class)
public interface GestoPagoCatProduct {

    @PostMapping("/sistema/service/getProductList.do")
    ResponseEntity<String> getProductList();
}
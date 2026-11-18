package com.proyecto.servicios.controller;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.CatalogoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final RestTemplate restTemplate = new RestTemplate();

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('SCOPE_READ')")
    @GetMapping(value = "/gestopago/catalogo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerCatalogo() {
        List<CatalogoProducto> catalogo = catalogoService.obtenerCatalogo();
        if (catalogo.isEmpty()) {
            GenericResponse respuesta = new GenericResponse();
            respuesta.setCodigo(1);
            respuesta.setMensaje("El catalogo de GestoPago no ha sido cargado aun en MongoDB");
            return new ResponseEntity<>(respuesta, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(catalogo, HttpStatus.OK);
    }

    /**
     * Endpoint proxy para servir las imágenes oficiales de GestoPago evitando bloqueos de CORS
     * en clientes Web (Flutter Web Canvas / HTML renderer).
     */
    @GetMapping(value = "/gestopago/logo/{idServicio}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> obtenerLogoServicio(@PathVariable String idServicio) {
        try {
            String url = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/" + idServicio + ".png";
            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
            if (imageBytes != null && imageBytes.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                headers.setCacheControl("public, max-age=86400");
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            }
        } catch (Exception ignored) {
        }
        return ResponseEntity.notFound().build();
    }
}
package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.CatalogoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

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
}
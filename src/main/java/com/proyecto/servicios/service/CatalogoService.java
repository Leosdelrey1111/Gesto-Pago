package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;

import java.util.List;

public interface CatalogoService {

    void reemplazarCatalogo(GestoPagoCatProductResponse response);

    List<CatalogoProducto> obtenerCatalogo();
}
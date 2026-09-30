package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;

public interface GestoPagoCatProductService {

    GestoPagoCatProductResponse obtenerCatalogoProductos();

    GestoPagoCatProductResponse obtenerCatalogoCache();
}
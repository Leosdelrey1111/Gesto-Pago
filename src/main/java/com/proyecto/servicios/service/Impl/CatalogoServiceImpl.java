package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;
import com.proyecto.servicios.model.gestopago.catproduct.Producto;
import com.proyecto.servicios.repository.gestopago.CatalogoProductoRepository;
import com.proyecto.servicios.service.CatalogoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CatalogoServiceImpl implements CatalogoService {

    private final CatalogoProductoRepository catalogoProductoRepository;

    public CatalogoServiceImpl(CatalogoProductoRepository catalogoProductoRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
    }

    @Override
    public void reemplazarCatalogo(GestoPagoCatProductResponse response) {
        if (response == null || response.getProductos() == null
                || response.getProductos().getProductos() == null) {
            throw new IllegalArgumentException("El catalogo a persistir no puede ser nulo");
        }

        List<Producto> productos = response.getProductos().getProductos();
        catalogoProductoRepository.deleteAll();
        List<CatalogoProducto> documentos = productos.stream()
                .map(this::aDocumento)
                .toList();
        catalogoProductoRepository.saveAll(documentos);
        log.info("Catalogo GestoPago persistido en MongoDB ({} productos)", documentos.size());
    }

    @Override
    public List<CatalogoProducto> obtenerCatalogo() {
        List<CatalogoProducto> lista = catalogoProductoRepository.findAll();
        boolean huboCambios = false;
        for (CatalogoProducto prod : lista) {
            if (prod.getUrlLogo() == null || prod.getUrlLogo().isBlank()) {
                prod.setUrlLogo(com.proyecto.servicios.util.LogoServicioResolver.resolverLogo(prod.getIdServicio(), prod.getServicio()));
                huboCambios = true;
            }
        }
        // Si habia registros antiguos sin urlLogo en MongoDB, los guardamos en bulk una sola vez
        if (huboCambios) {
            catalogoProductoRepository.saveAll(lista);
            log.info("MongoDB actualizado con las URLs de logos para {} productos", lista.size());
        }
        return lista;
    }

    private CatalogoProducto aDocumento(Producto producto) {
        CatalogoProducto documento = new CatalogoProducto();
        documento.setServicio(producto.getServicio());
        documento.setNombreProducto(producto.getNombreProducto());
        documento.setIdServicio(producto.getIdServicio());
        documento.setIdProducto(producto.getIdProducto());
        documento.setIdCatTipoServicio(producto.getIdCatTipoServicio());
        documento.setTipoFront(producto.getTipoFront());
        documento.setHasDigitoVerificador(producto.getHasDigitoVerificador());
        documento.setPrecio(producto.getPrecio());
        documento.setShowAyuda(producto.getShowAyuda());
        documento.setTipoReferencia(producto.getTipoReferencia());
        documento.setLegend(producto.getLegend());
        documento.setUrlLogo(com.proyecto.servicios.util.LogoServicioResolver.resolverLogo(producto.getIdServicio(), producto.getServicio()));
        return documento;
    }
}
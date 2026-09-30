package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;
import com.proyecto.servicios.model.gestopago.catproduct.Producto;
import com.proyecto.servicios.model.gestopago.catproduct.Productos;
import com.proyecto.servicios.model.gestopago.catproduct.Mensaje;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de CatalogoServiceImpl")
class CatalogoServiceImplTest {

    @Mock
    private CatalogoProductoRepository catalogoProductoRepository;

    @InjectMocks
    private CatalogoServiceImpl servicio;

    @Test
    @DisplayName("reemplazarCatalogo borra el catalogo anterior e inserta el nuevo")
    void reemplazarCatalogo_exito_borraEInserta() {
        GestoPagoCatProductResponse respuesta = respuestaConDosProductos();

        servicio.reemplazarCatalogo(respuesta);

        verify(catalogoProductoRepository, times(1)).deleteAll();

        ArgumentCaptor<List<CatalogoProducto>> captor = ArgumentCaptor.forClass(List.class);
        verify(catalogoProductoRepository, times(1)).saveAll(captor.capture());

        List<CatalogoProducto> documentos = captor.getValue();
        assertEquals(2, documentos.size());
        assertEquals("ABIB 100", documentos.get(0).getNombreProducto());
        assertEquals("14302", documentos.get(0).getIdProducto());
        assertEquals("100.0", documentos.get(0).getPrecio());
        assertEquals("soporte", documentos.get(0).getLegend());
    }

    @Test
    @DisplayName("reemplazarCatalogo con respuesta nula lanza excepcion")
    void reemplazarCatalogo_respuestaNula_lanza() {
        assertThrows(IllegalArgumentException.class, () -> servicio.reemplazarCatalogo(null));
    }

    @Test
    @DisplayName("reemplazarCatalogo sin productos lanza excepcion")
    void reemplazarCatalogo_sinProductos_lanza() {
        GestoPagoCatProductResponse respuesta = respuestaConDosProductos();
        respuesta.setProductos(null);

        assertThrows(IllegalArgumentException.class, () -> servicio.reemplazarCatalogo(respuesta));
    }

    @Test
    @DisplayName("obtenerCatalogo devuelve el catalogo persistido")
    void obtenerCatalogo_devuelveCatalogo() {
        CatalogoProducto documento = new CatalogoProducto();
        documento.setServicio("ABIB");
        documento.setNombreProducto("ABIB 100");
        when(catalogoProductoRepository.findAll()).thenReturn(List.of(documento));

        List<CatalogoProducto> catalogo = servicio.obtenerCatalogo();

        assertNotNull(catalogo);
        assertEquals(1, catalogo.size());
        assertEquals("ABIB 100", catalogo.get(0).getNombreProducto());
    }

    private static GestoPagoCatProductResponse respuestaConDosProductos() {
        GestoPagoCatProductResponse respuesta = new GestoPagoCatProductResponse();
        Mensaje mensaje = new Mensaje();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        respuesta.setMensaje(mensaje);
        respuesta.setProductos(new Productos());
        respuesta.getProductos().setProductos(List.of(producto("ABIB 100", "14302", "100.0"),
                producto("ABIB 130", "14305", "130.0")));
        return respuesta;
    }

    private static Producto producto(String nombre, String idProducto, String precio) {
        Producto producto = new Producto();
        producto.setServicio("ABIB");
        producto.setNombreProducto(nombre);
        producto.setIdServicio("2284");
        producto.setIdProducto(idProducto);
        producto.setIdCatTipoServicio("13");
        producto.setTipoFront("1");
        producto.setHasDigitoVerificador("false");
        producto.setPrecio(precio);
        producto.setShowAyuda("false");
        producto.setTipoReferencia("a");
        producto.setLegend("soporte");
        return producto;
    }
}
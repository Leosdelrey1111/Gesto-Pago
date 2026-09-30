package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoCatProduct;
import com.proyecto.servicios.exception.GestoPagoCatProductException;
import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;
import com.proyecto.servicios.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de GestoPagoCatProductServiceImpl")
class GestoPagoCatProductServiceImplTest {

    private static final String XML_CATALOGO = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<RESPONSE>"
            + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>"
            + "<PRODUCTOS>"
            + "<producto servicio='ABIB' producto='ABIB 100' idServicio='2284' idProducto='14302' "
            + "idCatTipoServicio='13' tipoFront='1' hasDigitoVerificador='false' precio='100.0' "
            + "showAyuda='false' tipoReferencia='a'><legend>soporte</legend></producto>"
            + "<producto servicio='ABIB' producto='ABIB 130' idServicio='2284' idProducto='14305' "
            + "idCatTipoServicio='13' tipoFront='1' hasDigitoVerificador='false' precio='130.0' "
            + "showAyuda='false' tipoReferencia='a'><legend>soporte</legend></producto>"
            + "</PRODUCTOS>"
            + "</RESPONSE>";

    private static final String XML_ECHO = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<RESPONSE><ECHO>GPS83</ECHO><VALID>1</VALID></RESPONSE>";

    @Mock
    private GestoPagoCatProduct gestoPagoCatProduct;

    @Mock
    private CatalogoService catalogoService;

    @InjectMocks
    private GestoPagoCatProductServiceImpl servicio;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(servicio, "inicioEsperaMs", 0L);
    }

    @Test
    @DisplayName("Respuesta 200 con XML valido devuelve el catalogo parseado")
    void obtenerCatalogoProductos_exito() {
        when(gestoPagoCatProduct.getProductList())
                .thenReturn(ResponseEntity.ok(XML_CATALOGO));

        GestoPagoCatProductResponse respuesta = servicio.obtenerCatalogoProductos();

        assertNotNull(respuesta);
        assertNotNull(respuesta.getMensaje());
        assertEquals("01", respuesta.getMensaje().getCodigo());
        assertNotNull(respuesta.getProductos());
        assertEquals(2, respuesta.getProductos().getProductos().size());
    }

    @Test
    @DisplayName("Respuesta HTTP distinta de 200 lanza excepcion tras los reintentos")
    void obtenerCatalogoProductos_respuestaNoExitosa() {
        when(gestoPagoCatProduct.getProductList())
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

        GestoPagoCatProductException ex = assertThrows(GestoPagoCatProductException.class,
                () -> servicio.obtenerCatalogoProductos());

        assertNotNull(ex.getMessage());
        verify(gestoPagoCatProduct, times(3)).getProductList();
    }

    @Test
    @DisplayName("Error de comunicacion con GestoPago lanza excepcion tras los reintentos")
    void obtenerCatalogoProductos_errorComunicacion() {
        when(gestoPagoCatProduct.getProductList())
                .thenThrow(new RuntimeException("connection refused"));

        GestoPagoCatProductException ex = assertThrows(GestoPagoCatProductException.class,
                () -> servicio.obtenerCatalogoProductos());

        verify(gestoPagoCatProduct, times(3)).getProductList();
    }

    @Test
    @DisplayName("Cuerpo vacio lanza excepcion")
    void obtenerCatalogoProductos_cuerpoVacio() throws Exception {
        when(gestoPagoCatProduct.getProductList()).thenReturn(ResponseEntity.ok(null));

        GestoPagoCatProductException ex = assertThrows(GestoPagoCatProductException.class,
                () -> servicio.obtenerCatalogoProductos());
    }

    @Test
    @DisplayName("XML sin mensaje de operacion lanza excepcion")
    void obtenerCatalogoProductos_xmlSinMensaje() {
        when(gestoPagoCatProduct.getProductList()).thenReturn(ResponseEntity.ok(XML_ECHO));

        assertThrows(GestoPagoCatProductException.class,
                () -> servicio.obtenerCatalogoProductos());
    }

    @Test
    @DisplayName("Carga programada con exito persiste el catalogo en MongoDB")
    void cargarCatalogoProgramado_exito_persiste() {
        when(gestoPagoCatProduct.getProductList())
                .thenReturn(ResponseEntity.ok(XML_CATALOGO));

        servicio.cargarCatalogoProgramado();

        verify(catalogoService, times(1)).reemplazarCatalogo(any(GestoPagoCatProductResponse.class));
        assertNotNull(servicio.obtenerCatalogoCache());
    }

    @Test
    @DisplayName("Carga programada sin exito no modifica el catalogo")
    void cargarCatalogoProgramado_sinExito_noModifica() {
        when(gestoPagoCatProduct.getProductList())
                .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no auth"));

        servicio.cargarCatalogoProgramado();

        verify(catalogoService, never()).reemplazarCatalogo(any(GestoPagoCatProductResponse.class));
        assertThrows(GestoPagoCatProductException.class, () -> servicio.obtenerCatalogoCache());
    }

    @Test
    @DisplayName("La carga inicial al arrancar consulta el catalogo y lo persiste")
    void cargarCatalogoAlInicio_consultaYPersiste() throws InterruptedException {
        when(gestoPagoCatProduct.getProductList())
                .thenReturn(ResponseEntity.ok(XML_CATALOGO));

        servicio.cargarCatalogoAlInicio();

        verify(catalogoService, timeout(5000)).reemplazarCatalogo(any(GestoPagoCatProductResponse.class));
    }

    @Test
    @DisplayName("Sin carga previa obtenerCatalogoCache lanza excepcion")
    void obtenerCatalogoCache_sinCarga_lanza() {
        assertThrows(GestoPagoCatProductException.class, () -> servicio.obtenerCatalogoCache());
    }
}
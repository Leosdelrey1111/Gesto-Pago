package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoCatProduct;
import com.proyecto.servicios.exception.GestoPagoCatProductException;
import com.proyecto.servicios.model.gestopago.catproduct.GestoPagoCatProductResponse;
import com.proyecto.servicios.service.CatalogoService;
import com.proyecto.servicios.service.GestoPagoCatProductService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GestoPagoCatProductServiceImpl implements GestoPagoCatProductService {

    private static final int MAX_REINTENTOS = 3;
    private static final long ESPERA_ENTRE_REINTENTOS_MS = 500L;
    private static final JAXBContext JAXB_CONTEXT = crearJaxbContext();

    private final GestoPagoCatProduct gestoPagoCatProduct;
    private final CatalogoService catalogoService;

    @Value("${gestopago.catalogo.startup-delay-ms:3000}")
    private long inicioEsperaMs;

    private volatile GestoPagoCatProductResponse catalogoCache;

    public GestoPagoCatProductServiceImpl(GestoPagoCatProduct gestoPagoCatProduct,
                                          CatalogoService catalogoService) {
        this.gestoPagoCatProduct = gestoPagoCatProduct;
        this.catalogoService = catalogoService;
    }

    @Override
    public GestoPagoCatProductResponse obtenerCatalogoProductos() {
        int intento = 0;
        while (intento < MAX_REINTENTOS) {
            intento++;
            log.info("Obteniendo catalogo de productos GestoPago, intento {}/{}", intento, MAX_REINTENTOS);
            try {
                GestoPagoCatProductResponse response = llamarYValidar();
                log.info("Catalogo de productos obtenido correctamente ({} productos)",
                        response.getProductos().getProductos().size());
                return response;
            } catch (GestoPagoCatProductException ex) {
                log.error("Intento {}/{} fallido: {}", intento, MAX_REINTENTOS, ex.getMessage());
                if (intento == MAX_REINTENTOS) {
                    throw ex;
                }
                esperar();
            } catch (Exception ex) {
                log.error("Intento {}/{} fallido por error inesperado: {}", intento, MAX_REINTENTOS, ex.getMessage(), ex);
                if (intento == MAX_REINTENTOS) {
                    throw new GestoPagoCatProductException(
                            "No fue posible obtener el catalogo de GestoPago tras " + MAX_REINTENTOS + " intentos", ex);
                }
                esperar();
            }
        }
        throw new GestoPagoCatProductException(
                "No fue posible obtener el catalogo de GestoPago tras " + MAX_REINTENTOS + " intentos");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cargarCatalogoAlInicio() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(inicioEsperaMs);
                cargarCatalogoProgramado();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Carga inicial del catalogo interrumpida: {}", ex.getMessage());
            }
        });
    }

    @Scheduled(cron = "${gestopago.catalogo.cron:0 0 6 * * *}")
    public void cargarCatalogoProgramado() {
        log.info("Ejecutando carga programada del catalogo GestoPago (06:00 hrs)");
        try {
            GestoPagoCatProductResponse response = obtenerCatalogoProductos();
            this.catalogoCache = response;
            catalogoService.reemplazarCatalogo(response);
            log.info("Catalogo GestoPago cargado y persistido en MongoDB ({} productos)",
                    response.getProductos().getProductos().size());
        } catch (GestoPagoCatProductException ex) {
            log.error("No fue posible poblar el catalogo en la ejecucion programada: {}", ex.getMessage());
        }
    }

    @Override
    public GestoPagoCatProductResponse obtenerCatalogoCache() {
        GestoPagoCatProductResponse cache = this.catalogoCache;
        if (cache == null) {
            throw new GestoPagoCatProductException(
                    "El catalogo de GestoPago aun no ha sido cargado por el proceso programado de las 06:00 hrs");
        }
        return cache;
    }

    private GestoPagoCatProductResponse llamarYValidar() {
        ResponseEntity<String> respuestaHttp;
        try {
            respuestaHttp = gestoPagoCatProduct.getProductList();
        } catch (Exception ex) {
            throw new GestoPagoCatProductException("Error de conexion con el servicio GestoPago: " + ex.getMessage(), ex);
        }

        if (respuestaHttp == null || !HttpStatusCode.valueOf(200).equals(respuestaHttp.getStatusCode())) {
            int codigoHttp = respuestaHttp == null ? -1 : respuestaHttp.getStatusCode().value();
            throw new GestoPagoCatProductException(
                    "El servicio GestoPago respondio con codigo HTTP " + codigoHttp
                            + "; no se modifica el catalogo, se conserva el anterior");
        }

        String xml = respuestaHttp.getBody();
        if (xml == null || xml.isBlank()) {
            throw new GestoPagoCatProductException("El servicio GestoPago respondio con un cuerpo vacio");
        }

        try {
            GestoPagoCatProductResponse response = deserializar(xml);
            validar(response);
            return response;
        } catch (JAXBException ex) {
            log.warn("XML recibido de GestoPago que no pudo mapearse (truncado): {}", truncar(xml));
            throw new GestoPagoCatProductException("No fue posible interpretar el XML devuelto por GestoPago", ex);
        } catch (GestoPagoCatProductException ex) {
            log.warn("XML recibido de GestoPago que no paso la validacion (truncado): {}", truncar(xml));
            throw ex;
        }
    }

    private static String truncar(String xml) {
        int max = 2000;
        return xml.length() <= max ? xml : xml.substring(0, max);
    }

    private void validar(GestoPagoCatProductResponse response) {
        if (response == null) {
            throw new GestoPagoCatProductException("El catalogo de GestoPago llego vacio o nulo");
        }
        if (response.getMensaje() == null || response.getMensaje().getCodigo() == null) {
            throw new GestoPagoCatProductException("La respuesta de GestoPago no contiene el mensaje de operacion");
        }
        if (response.getProductos() == null || response.getProductos().getProductos() == null
                || response.getProductos().getProductos().isEmpty()) {
            throw new GestoPagoCatProductException("El catalogo de GestoPago no contiene productos");
        }
    }

    private static GestoPagoCatProductResponse deserializar(String xml) throws JAXBException {
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        return (GestoPagoCatProductResponse) unmarshaller.unmarshal(new StringReader(xml));
    }

    private void esperar() {
        try {
            Thread.sleep(ESPERA_ENTRE_REINTENTOS_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GestoPagoCatProductException("El reintento fue interrumpido", ex);
        }
    }

    private static JAXBContext crearJaxbContext() {
        try {
            return JAXBContext.newInstance(GestoPagoCatProductResponse.class);
        } catch (JAXBException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
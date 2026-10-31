package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.exception.MontoNoPermitidoException;
import com.proyecto.servicios.exception.OperacionNoProcesableException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.transaccion.PagarServicioRequestDTO;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoPagoValidator: producto, servicio y monto contra catalogo")
class ProductoPagoValidatorTest {

    @Mock
    private CatalogoProductoRepository catalogoRepository;

    private ProductoPagoValidator validator;
    private CatalogoProducto producto;

    @BeforeEach
    void setUp() {
        GestoPagoTxProperties props = new GestoPagoTxProperties();
        props.setMontoMinimo(new BigDecimal("10.00"));
        props.setMontoMaximo(new BigDecimal("5000.00"));
        validator = new ProductoPagoValidator(catalogoRepository, props);

        producto = new CatalogoProducto();
        producto.setIdProducto("582");
        producto.setIdServicio("133");
        producto.setNombreProducto("Recarga 100");
        producto.setTipoFront("1");
        producto.setIdCatTipoServicio("1");
        producto.setPrecio("100.0");
    }

    private PagarServicioRequestDTO request(BigDecimal monto, String destino) {
        return PagarServicioRequestDTO.builder()
                .numeroCuenta("0000000001").idProducto("582").idServicio("133")
                .telefonoOreferencia(destino).monto(monto).build();
    }

    private void catalogo() {
        when(catalogoRepository.findByIdProductoAndIdServicio("582", "133")).thenReturn(Optional.of(producto));
    }

    @Test
    @DisplayName("Producto inexistente en catalogo -> ResourceNotFoundException")
    void productoInexistente() {
        when(catalogoRepository.findByIdProductoAndIdServicio("582", "133")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> validator.validar(request(null, "5512345678")));
    }

    @Test
    @DisplayName("Precio fijo: sin monto en la peticion se cobra el precio del catalogo")
    void precioFijoSinMonto() {
        catalogo();
        PagoPlan plan = validator.validar(request(null, "5512345678"));
        assertEquals(new BigDecimal("100.00"), plan.monto());
        assertFalse(plan.montoAbierto());
        assertEquals("5512345678", plan.telefono());
        assertNull(plan.referencia());
    }

    @Test
    @DisplayName("Precio fijo: un monto distinto al del catalogo (manipulado) se rechaza")
    void precioFijoManipulado() {
        catalogo();
        assertThrows(MontoNoPermitidoException.class, () -> validator.validar(request(new BigDecimal("1.00"), "5512345678")));
    }

    @Test
    @DisplayName("Precio fijo: el monto coincidente en otra escala (100 vs 100.00) es valido")
    void precioFijoEscalaDistinta() {
        catalogo();
        assertEquals(new BigDecimal("100.00"), validator.validar(request(new BigDecimal("100"), "5512345678")).monto());
    }

    @Test
    @DisplayName("Precio fijo con precio de catalogo vacio, invalido o no positivo -> error de negocio")
    void precioCatalogoInvalido() {
        for (String precio : new String[]{null, " ", "abc", "0", "-5"}) {
            producto.setPrecio(precio);
            catalogo();
            assertThrows(OperacionNoProcesableException.class,
                    () -> validator.validar(request(null, "5512345678")), "precio=" + precio);
        }
    }

    @Test
    @DisplayName("Monto abierto: obligatorio")
    void montoAbiertoRequerido() {
        producto.setTipoFront("2");
        catalogo();
        assertThrows(MontoNoPermitidoException.class, () -> validator.validar(request(null, "REF123")));
    }

    @Test
    @DisplayName("Monto abierto: respeta minimo y maximo configurados")
    void montoAbiertoLimites() {
        producto.setTipoFront("2");
        for (String m : new String[]{"9.99", "5000.01"}) {
            catalogo();
            assertThrows(MontoNoPermitidoException.class,
                    () -> validator.validar(request(new BigDecimal(m), "REF123")), "monto=" + m);
        }
        catalogo();
        PagoPlan plan = validator.validar(request(new BigDecimal("250.5"), "REF123"));
        assertEquals(new BigDecimal("250.50"), plan.monto());
        assertTrue(plan.montoAbierto());
        assertEquals("1111111111", plan.telefono());
        assertEquals("REF123", plan.referencia());
    }

    @Test
    @DisplayName("Monto abierto con mas de 2 decimales significativos se rechaza")
    void montoAbiertoDecimales() {
        producto.setTipoFront("2");
        catalogo();
        assertThrows(MontoNoPermitidoException.class, () -> validator.validar(request(new BigDecimal("20.555"), "REF123")));
    }

    @Test
    @DisplayName("Servicios de referencia (tipo 10/11) usan telefono comodin y envian la referencia")
    void servicioDeReferencia() {
        producto.setIdCatTipoServicio("10");
        catalogo();
        PagoPlan plan = validator.validar(request(null, "REFABC123"));
        assertEquals("#1111111111", plan.telefono());
        assertEquals("REFABC123", plan.referencia());
    }

    @Test
    @DisplayName("Recargas exigen un telefono de exactamente 10 digitos")
    void telefonoInvalido() {
        catalogo();
        assertThrows(ReglaNegocioException.class, () -> validator.validar(request(null, "12345")));
    }
}
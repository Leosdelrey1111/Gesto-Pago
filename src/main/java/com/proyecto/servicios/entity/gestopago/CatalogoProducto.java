package com.proyecto.servicios.entity.gestopago;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "catalogo_productos")
public class CatalogoProducto {

    @Id
    private String id;

    private String servicio;
    private String nombreProducto;
    private String idServicio;
    private String idProducto;
    private String idCatTipoServicio;
    private String tipoFront;
    private String hasDigitoVerificador;
    private String precio;
    private String showAyuda;
    private String tipoReferencia;
    private String legend;
}
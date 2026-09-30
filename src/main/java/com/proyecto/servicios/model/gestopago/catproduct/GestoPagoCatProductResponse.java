package com.proyecto.servicios.model.gestopago.catproduct;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoCatProductResponse {

    @XmlElement(name = "MENSAJE")
    private Mensaje mensaje;

    @XmlElement(name = "PRODUCTOS")
    private Productos productos;
}
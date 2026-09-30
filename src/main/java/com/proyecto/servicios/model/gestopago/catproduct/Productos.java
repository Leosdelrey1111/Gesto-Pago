package com.proyecto.servicios.model.gestopago.catproduct;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Productos {

    @XmlElement(name = "producto")
    private List<Producto> productos;
}
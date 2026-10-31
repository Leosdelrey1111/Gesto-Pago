package com.proyecto.servicios.model.gestopago.tx;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoTxResponse {

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "MENSAJE")
    private GestoPagoTxMensaje mensaje;
}

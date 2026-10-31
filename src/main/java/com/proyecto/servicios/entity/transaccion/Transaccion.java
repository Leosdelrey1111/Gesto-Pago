package com.proyecto.servicios.entity.transaccion;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones",
        indexes = {
                @Index(name = "idx_tx_upc", columnList = "upc"),
                @Index(name = "idx_tx_cuenta", columnList = "numero_cuenta"),
                @Index(name = "idx_tx_estatus", columnList = "estatus"),
                @Index(name = "idx_tx_huella", columnList = "numero_cuenta,huella_solicitud,fecha_creacion")
        },
        uniqueConstraints = {
                // Idempotencia: la misma clave nunca puede crear dos cargos para la misma cuenta.
                @UniqueConstraint(name = "uk_tx_cuenta_clave", columnNames = {"numero_cuenta", "clave_idempotencia"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, length = 50)
    private String numeroCuenta;

    @Column(nullable = false, unique = true, length = 100)
    private String upc;

    /** Clave enviada por el cliente para que un reintento devuelva el mismo resultado. */
    @Column(name = "clave_idempotencia", length = 64)
    private String claveIdempotencia;

    /** SHA-256 de los datos de negocio de la solicitud; detecta claves reutilizadas con datos distintos y duplicados. */
    @Column(name = "huella_solicitud", length = 64)
    private String huellaSolicitud;

    @Column(name = "id_producto", nullable = false, length = 50)
    private String idProducto;

    @Column(name = "id_servicio", nullable = false, length = 50)
    private String idServicio;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(length = 50)
    private String telefono;

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo", precision = 15, scale = 2)
    private BigDecimal saldoNuevo;

    /** JSON sin cifrar enviado al proveedor (sin secretos). Permite reconstruir el reenvio con el mismo UPC. */
    @Column(name = "payload_enviado", columnDefinition = "TEXT")
    private String payloadEnviado;

    @Column(name = "id_tx_gestopago", length = 100)
    private String idTxGestoPago;

    @Column(name = "num_autorizacion", length = 100)
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstatusTransaccion estatus;

    @Column(name = "intentos_conciliacion", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer intentosConciliacion = 0;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}

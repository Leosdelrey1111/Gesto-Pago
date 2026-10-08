package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoCivil;
import com.proyecto.servicios.entity.onboarding.catalogos.Nacionalidad;
import com.proyecto.servicios.entity.onboarding.catalogos.Sexo;
import com.proyecto.servicios.entity.onboarding.catalogos.TipoBaja;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_cliente_curp", columnList = "curp"),
        @Index(name = "idx_cliente_rfc", columnList = "rfc"),
        @Index(name = "idx_cliente_correo", columnList = "correo_electronico"),
        @Index(name = "idx_cliente_nombre", columnList = "nombre"),
        @Index(name = "idx_cliente_apaterno", columnList = "apellido_paterno"),
        @Index(name = "idx_cliente_amaterno", columnList = "apellido_materno")
})
@org.hibernate.annotations.Check(constraints = "ingreso_mensual > 0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 50)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, unique = true, length = 18, columnDefinition = "CHAR(18)")
    private String curp;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String rfc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexo sexo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Nacionalidad nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil", nullable = false, length = 20)
    private EstadoCivil estadoCivil;

    @Column(name = "correo_electronico", nullable = false, unique = true, columnDefinition = "TEXT")
    private String correoElectronico;

    @Column(name = "lada_movil", nullable = false)
    private Integer ladaMovil;

    @Column(name = "telefono_movil", nullable = false)
    private Long telefonoMovil;

    @Column(name = "lada_alternativo")
    private Integer ladaAlternativo;

    @Column(name = "telefono_alternativo")
    private Long telefonoAlternativo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ocupacion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean bloqueado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_baja", length = 50)
    private TipoBaja tipoBaja;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    // Relaciones Bidireccionales
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Domicilio domicilio;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Cuenta> cuentas = new ArrayList<>();
}

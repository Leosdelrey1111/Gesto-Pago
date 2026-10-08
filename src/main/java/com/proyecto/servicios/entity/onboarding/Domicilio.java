package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.entity.onboarding.catalogos.EstadoMexico;
import com.proyecto.servicios.entity.onboarding.catalogos.Pais;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domicilios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Cliente cliente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String colonia;

    @Column(nullable = false, length = 100)
    private String municipio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoMexico estado;

    @Column(name = "codigo_postal", nullable = false)
    private Integer codigoPostal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Pais pais;
}

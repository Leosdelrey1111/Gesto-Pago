package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.entity.onboarding.catalogos.EstatusCuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas", indexes = {
        @Index(name = "idx_cuenta_numero", columnList = "numero_cuenta"),
        @Index(name = "idx_cuenta_estatus", columnList = "estatus")
})
@org.hibernate.annotations.Check(constraints = "saldo >= 0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, columnDefinition = "TEXT")
    private String numeroCuenta;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstatusCuenta estatus = EstatusCuenta.ACTIVA;
}

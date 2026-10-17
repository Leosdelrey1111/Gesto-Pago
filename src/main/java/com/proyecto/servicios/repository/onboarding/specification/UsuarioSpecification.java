package com.proyecto.servicios.repository.onboarding.specification;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    public static Specification<Usuario> conFiltros(UsuarioBusquedaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), filtro.getClienteId()));
            }

            if (filtro.getCorreo() != null && !filtro.getCorreo().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("correo")), "%" + filtro.getCorreo().trim().toLowerCase() + "%"));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            if (filtro.getFechaCreacionDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaCreacionDesde()));
            }

            if (filtro.getFechaCreacionHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaCreacionHasta()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

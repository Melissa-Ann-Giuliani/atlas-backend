package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.usuario.Usuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class CambioSpecification {

    private CambioSpecification() {
    }

    public static Specification<Cambio> withFilters(
            String usuarioAfectado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            TipoOperacion tipoOperacion,
            Integer usuarioId,
            List<String> tablasAfectadas,
            Integer unidadId,
            boolean requireUsuarioAfectado) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (requireUsuarioAfectado) {
                predicates.add(cb.isNotNull(root.get("usuarioAfectado")));
            }

            if (unidadId != null) {
                predicates.add(cb.equal(root.get("unidad").get("unidadId"), unidadId));
            }

            if (usuarioAfectado != null && !usuarioAfectado.isBlank()) {
                String pattern = "%" + usuarioAfectado.trim().toLowerCase() + "%";
                Join<Cambio, Usuario> affectedJoin = root.join("usuarioAfectado", JoinType.INNER);
                Predicate byNombre = cb.like(cb.lower(affectedJoin.get("nombre")), pattern);
                Predicate byApellido = cb.like(cb.lower(affectedJoin.get("apellido")), pattern);
                Predicate byFullName = cb.like(
                        cb.lower(cb.concat(cb.concat(affectedJoin.get("apellido"), " "), affectedJoin.get("nombre"))),
                        pattern);
                Predicate byReverseFullName = cb.like(
                        cb.lower(cb.concat(cb.concat(affectedJoin.get("nombre"), " "), affectedJoin.get("apellido"))),
                        pattern);
                predicates.add(cb.or(byNombre, byApellido, byFullName, byReverseFullName));
            }

            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), fechaDesde.atStartOfDay()));
            }

            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaHora"), fechaHasta.atTime(LocalTime.MAX)));
            }

            if (tipoOperacion != null) {
                predicates.add(cb.equal(root.get("tipoOperacion"), tipoOperacion));
            }

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (tablasAfectadas != null && !tablasAfectadas.isEmpty()) {
                predicates.add(root.get("tablaAfectada").in(tablasAfectadas));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

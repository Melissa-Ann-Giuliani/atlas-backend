package com.atlas.atlas_backend.dashboard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countTotalTeachers(
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(DISTINCT d.usuarioId) " +
                "FROM Cargo c JOIN c.designacion d JOIN Docente doc ON doc.id = d.usuarioId JOIN c.unidad u ");
        Map<String, Object> params = appendFilters(
                jpql, unidadId, caracterIds, categoriaIds, dedicacionIds, origenIds, estados);

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    public List<Object[]> countTeachersByUnitType(
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        StringBuilder jpql = new StringBuilder(
                "SELECT tu.nombre, COUNT(DISTINCT d.usuarioId) " +
                "FROM Cargo c JOIN c.designacion d JOIN Docente doc ON doc.id = d.usuarioId JOIN c.unidad u JOIN u.tipoUnidad tu ");
        Map<String, Object> params = appendFilters(
                jpql, unidadId, caracterIds, categoriaIds, dedicacionIds, origenIds, estados);
        jpql.append(" GROUP BY tu.nombre");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public List<Object[]> countTeachersByUnit(
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        StringBuilder jpql = new StringBuilder(
                "SELECT tu.nombre, u.unidadNombre, COUNT(DISTINCT d.usuarioId) " +
                "FROM Cargo c JOIN c.designacion d JOIN Docente doc ON doc.id = d.usuarioId JOIN c.unidad u JOIN u.tipoUnidad tu ");
        Map<String, Object> params = appendFilters(
                jpql, unidadId, caracterIds, categoriaIds, dedicacionIds, origenIds, estados);
        jpql.append(" GROUP BY tu.nombre, u.unidadNombre");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public List<Object[]> findActiveMateriasByUnidad(Integer unidadId) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT m.id, m.nombre " +
                "FROM Materia m " +
                "WHERE m.unidad.unidadId = :unidadId AND LOWER(m.estado) = 'activa' " +
                "ORDER BY m.nombre ASC, m.id ASC",
                Object[].class);
        query.setParameter("unidadId", unidadId);
        return query.getResultList();
    }

    public List<Object[]> countTeachersByMateria(
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        StringBuilder jpql = new StringBuilder(
                "SELECT m.id, COUNT(DISTINCT d.usuarioId) " +
                "FROM Funcion f JOIN f.materia m JOIN f.cargo c JOIN c.designacion d " +
                "JOIN Docente doc ON doc.id = d.usuarioId JOIN c.unidad u ");
        Map<String, Object> params = appendFilters(
                jpql, unidadId, caracterIds, categoriaIds, dedicacionIds, origenIds, estados);
        jpql.append(" AND m.unidad.unidadId = :materiaUnidadId AND LOWER(m.estado) = 'activa'");
        params.put("materiaUnidadId", unidadId);
        jpql.append(" GROUP BY m.id");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public Object[] findUnidadMetadata(Integer unidadId) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT u.unidadNombre, tu.nombre " +
                "FROM Unidad u LEFT JOIN u.tipoUnidad tu " +
                "WHERE u.unidadId = :unidadId",
                Object[].class);
        query.setParameter("unidadId", unidadId);
        List<Object[]> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private Map<String, Object> appendFilters(
            StringBuilder jpql,
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        Map<String, Object> params = new HashMap<>();
        jpql.append("WHERE LOWER(doc.estado) IN ('activo', 'licencia')");

        if (unidadId != null) {
            jpql.append(" AND u.unidadId = :unidadId");
            params.put("unidadId", unidadId);
        }
        if (caracterIds != null && !caracterIds.isEmpty()) {
            jpql.append(" AND c.caracter.caracterId IN :caracterIds");
            params.put("caracterIds", caracterIds);
        }
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            jpql.append(" AND c.categoria.categoriaId IN :categoriaIds");
            params.put("categoriaIds", categoriaIds);
        }
        if (dedicacionIds != null && !dedicacionIds.isEmpty()) {
            jpql.append(" AND c.dedicacion.dedicacionId IN :dedicacionIds");
            params.put("dedicacionIds", dedicacionIds);
        }
        if (origenIds != null && !origenIds.isEmpty()) {
            jpql.append(" AND c.origen.origenId IN :origenIds");
            params.put("origenIds", origenIds);
        }
        if (estados != null && !estados.isEmpty()) {
            boolean includeLicencia = estados.contains("licencia");
            boolean includeActivo = estados.contains("activo");
            if (includeLicencia && !includeActivo) {
                jpql.append(" AND EXISTS (" + ACTIVE_LICENCIA_SUBQUERY + ")");
            } else if (includeActivo && !includeLicencia) {
                jpql.append(" AND NOT EXISTS (" + ACTIVE_LICENCIA_SUBQUERY + ")");
            } else if (!includeActivo && !includeLicencia) {
                jpql.append(" AND 1 = 0");
            }
        }
        return params;
    }

    private static final String ACTIVE_LICENCIA_SUBQUERY =
            "SELECT 1 FROM Licencia l " +
            "WHERE l.designacion = d " +
            "AND LOWER(d.designacionEstadoActual) = 'licencia' " +
            "AND CURRENT_DATE >= l.fechaInicio " +
            "AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))";
}

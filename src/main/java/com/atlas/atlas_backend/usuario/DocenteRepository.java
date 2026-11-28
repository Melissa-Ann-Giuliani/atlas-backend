package com.atlas.atlas_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    boolean existsByDni(Long dni);

    boolean existsByCuil(Long cuil);

    boolean existsByLegajo(Integer legajo);

    boolean existsByLegajoAndIdNot(Integer legajo, Integer id);

    boolean existsByEmailInstitucional(String emailInstitucional);

    boolean existsByEmailInstitucionalAndIdNot(String emailInstitucional, Integer id);

    java.util.List<Docente> findByEstadoIgnoreCase(String estado);

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT d.id AS usuarioId, d.legajo AS legajo, d.nombre AS nombre, d.apellido AS apellido,
                   o.origenNombre AS origen, u.unidadNombre AS unidad, u.tipoUnidad.nombre AS tipoUnidadNombre, cat.categoriaNombre AS categoria,
                   ded.dedicacionNombre AS dedicacion, car.caracterNombre AS caracter,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM Licencia l
                       WHERE l.designacion = des
                         AND LOWER(des.designacionEstadoActual) = 'licencia'
                         AND CURRENT_DATE >= l.fechaInicio
                         AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))
                   ) THEN 'Licencia' ELSE 'Activo' END AS estado
            FROM Cargo c
            JOIN c.designacion des
            JOIN Docente d ON d.id = des.usuarioId
            JOIN c.origen o
            JOIN c.unidad u
            JOIN c.categoria cat
            JOIN c.dedicacion ded
            JOIN c.caracter car
            WHERE LOWER(d.estado) IN ('activo', 'licencia')
              AND (CAST(:estado AS string) IS NULL
                   OR (LOWER(CAST(:estado AS string)) = 'licencia' AND EXISTS (
                       SELECT 1 FROM Licencia l
                       WHERE l.designacion = des
                         AND LOWER(des.designacionEstadoActual) = 'licencia'
                         AND CURRENT_DATE >= l.fechaInicio
                         AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))
                   ))
                   OR (LOWER(CAST(:estado AS string)) = 'activo' AND NOT EXISTS (
                       SELECT 1 FROM Licencia l
                       WHERE l.designacion = des
                         AND LOWER(des.designacionEstadoActual) = 'licencia'
                         AND CURRENT_DATE >= l.fechaInicio
                         AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))
                   )))
              AND (:unidadId IS NULL OR u.unidadId = :unidadId)
              AND (CAST(:searchTerm AS string) IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(u.unidadNombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                   OR CAST(d.dni AS string) LIKE CONCAT('%', CAST(:searchTerm AS string), '%')
                   OR CAST(d.legajo AS string) LIKE CONCAT('%', CAST(:searchTerm AS string), '%'))
              AND (:origen IS NULL OR o.origenNombre = :origen)
              AND (:dedicacion IS NULL OR ded.dedicacionNombre = :dedicacion)
              AND (:categoria IS NULL OR cat.categoriaNombre = :categoria)
              AND (:caracter IS NULL OR car.caracterNombre = :caracter)
              AND (:tipoUnidad IS NULL OR u.tipoUnidad.nombre = :tipoUnidad)
        """,
        countQuery = """
            SELECT count(c)
            FROM Cargo c
            JOIN c.designacion des
            JOIN Docente d ON d.id = des.usuarioId
            JOIN c.origen o
            JOIN c.unidad u
            JOIN c.categoria cat
            JOIN c.dedicacion ded
            JOIN c.caracter car
            WHERE LOWER(d.estado) IN ('activo', 'licencia')
              AND (CAST(:estado AS string) IS NULL
                   OR (LOWER(CAST(:estado AS string)) = 'licencia' AND EXISTS (
                       SELECT 1 FROM Licencia l
                       WHERE l.designacion = des
                         AND LOWER(des.designacionEstadoActual) = 'licencia'
                         AND CURRENT_DATE >= l.fechaInicio
                         AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))
                   ))
                   OR (LOWER(CAST(:estado AS string)) = 'activo' AND NOT EXISTS (
                       SELECT 1 FROM Licencia l
                       WHERE l.designacion = des
                         AND LOWER(des.designacionEstadoActual) = 'licencia'
                         AND CURRENT_DATE >= l.fechaInicio
                         AND (COALESCE(l.fechaFin, l.fechaFinEstim) IS NULL OR CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim))
                   )))
              AND (:unidadId IS NULL OR u.unidadId = :unidadId)
              AND (CAST(:searchTerm AS string) IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(u.unidadNombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                   OR CAST(d.dni AS string) LIKE CONCAT('%', CAST(:searchTerm AS string), '%')
                   OR CAST(d.legajo AS string) LIKE CONCAT('%', CAST(:searchTerm AS string), '%'))
              AND (:origen IS NULL OR o.origenNombre = :origen)
              AND (:dedicacion IS NULL OR ded.dedicacionNombre = :dedicacion)
              AND (:categoria IS NULL OR cat.categoriaNombre = :categoria)
              AND (:caracter IS NULL OR car.caracterNombre = :caracter)
              AND (:tipoUnidad IS NULL OR u.tipoUnidad.nombre = :tipoUnidad)
        """
    )
    org.springframework.data.domain.Page<DocenteListDTO> findActiveDocentes(
            @org.springframework.data.repository.query.Param("unidadId") Integer unidadId,
            @org.springframework.data.repository.query.Param("searchTerm") String searchTerm,
            @org.springframework.data.repository.query.Param("origen") String origen,
            @org.springframework.data.repository.query.Param("dedicacion") String dedicacion,
            @org.springframework.data.repository.query.Param("categoria") String categoria,
            @org.springframework.data.repository.query.Param("caracter") String caracter,
            @org.springframework.data.repository.query.Param("tipoUnidad") String tipoUnidad,
            @org.springframework.data.repository.query.Param("estado") String estado,
            org.springframework.data.domain.Pageable pageable);
}

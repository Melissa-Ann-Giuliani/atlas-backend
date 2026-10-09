package com.atlas.atlas_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    boolean existsByDni(Long dni);

    boolean existsByCuil(Long cuil);

    boolean existsByEmailInstitucional(String emailInstitucional);

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT d.id AS usuarioId, d.nombre AS nombre, d.apellido AS apellido,
                   o.origenNombre AS origen, u.unidadNombre AS unidad, u.tipoUnidad.nombre AS tipoUnidadNombre, cat.categoriaNombre AS categoria,
                   ded.dedicacionNombre AS dedicacion, car.caracterNombre AS caracter, d.estado AS estado
            FROM Cargo c
            JOIN c.designacion des
            JOIN Docente d ON d.id = des.usuarioId
            JOIN c.origen o
            JOIN c.unidad u
            JOIN c.categoria cat
            JOIN c.dedicacion ded
            JOIN c.caracter car
            WHERE d.estado IN ('Activo', 'Licencia')
              AND (:estado IS NULL OR d.estado = :estado)
              AND (:unidadId IS NULL OR u.unidadId = :unidadId)
              AND (CAST(:searchTerm AS string) IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(u.unidadNombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')))
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
            WHERE d.estado IN ('Activo', 'Licencia')
              AND (:estado IS NULL OR d.estado = :estado)
              AND (:unidadId IS NULL OR u.unidadId = :unidadId)
              AND (CAST(:searchTerm AS string) IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) 
                   OR LOWER(u.unidadNombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')))
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

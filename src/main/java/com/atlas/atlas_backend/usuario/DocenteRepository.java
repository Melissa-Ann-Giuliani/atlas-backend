package com.atlas.atlas_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    boolean existsByDni(Long dni);

    boolean existsByCuil(Long cuil);

    boolean existsByEmailInstitucional(String emailInstitucional);

    @org.springframework.data.jpa.repository.Query("""
        SELECT d.id AS usuarioId, d.nombre AS nombre, d.apellido AS apellido,
               o.origenNombre AS origen, u.unidadNombre AS unidad, cat.categoriaNombre AS categoria,
               ded.dedicacionNombre AS dedicacion, car.caracterNombre AS caracter, des.designacionEstadoActual AS estado
        FROM Docente d
        JOIN Designacion des ON des.usuarioId = d.id
        JOIN Cargo c ON c.designacion.designacionId = des.designacionId
        JOIN c.origen o
        JOIN c.unidad u
        JOIN c.categoria cat
        JOIN c.dedicacion ded
        JOIN c.caracter car
        WHERE des.designacionEstadoActual = 'Activo'
          AND (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (:searchTerm IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR LOWER(u.unidadNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
          AND (:origen IS NULL OR o.origenNombre = :origen)
          AND (:dedicacion IS NULL OR ded.dedicacionNombre = :dedicacion)
    """)
    org.springframework.data.domain.Page<DocenteListDTO> findActiveDocentes(
            @org.springframework.data.repository.query.Param("unidadId") Integer unidadId,
            @org.springframework.data.repository.query.Param("searchTerm") String searchTerm,
            @org.springframework.data.repository.query.Param("origen") String origen,
            @org.springframework.data.repository.query.Param("dedicacion") String dedicacion,
            org.springframework.data.domain.Pageable pageable);
}

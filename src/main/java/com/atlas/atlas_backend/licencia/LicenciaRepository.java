package com.atlas.atlas_backend.licencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Integer> {
    List<Licencia> findByDesignacionDesignacionIdIn(List<Integer> designacionIds);

    @Query("""
        SELECT l.id AS id,
               l.nroResolucion AS nroResolucion,
               m.nombre AS motivoNombre,
               CONCAT(doc.apellido, ', ', doc.nombre) AS docenteNombreCompleto,
               l.fechaInicio AS fechaInicio,
               COALESCE(l.fechaFin, l.fechaFinEstim) AS fechaFin,
               l.cantidadDias AS diasUsados,
               'Activa' AS estado,
               doc.id AS docenteId
        FROM Licencia l
        JOIN l.motivo m
        JOIN l.designacion des
        JOIN Docente doc ON doc.id = des.usuarioId
        WHERE des.designacionEstadoActual = 'Licencia'
          AND CURRENT_DATE >= l.fechaInicio
          AND CURRENT_DATE <= COALESCE(l.fechaFin, l.fechaFinEstim)
          AND (:motivoId IS NULL OR m.id = :motivoId)
          AND (:fechaDesde IS NULL OR l.fechaInicio >= :fechaDesde)
          AND (:fechaHasta IS NULL OR COALESCE(l.fechaFin, l.fechaFinEstim) <= :fechaHasta)
          AND (:searchTerm IS NULL OR :searchTerm = ''
               OR LOWER(l.nroResolucion) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(doc.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(doc.apellido) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(CONCAT(doc.apellido, ', ', doc.nombre)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
          AND EXISTS (
              SELECT 1 FROM Cargo c
              WHERE c.designacion.designacionId = des.designacionId
                AND (:unidadId IS NULL OR c.unidad.unidadId = :unidadId)
          )
    """)
    Page<LicenciaListDTO> findActiveLicencias(
            @Param("unidadId") Integer unidadId,
            @Param("searchTerm") String searchTerm,
            @Param("motivoId") Integer motivoId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);
}

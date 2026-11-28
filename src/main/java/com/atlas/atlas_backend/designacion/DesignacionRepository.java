package com.atlas.atlas_backend.designacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DesignacionRepository extends JpaRepository<Designacion, Integer> {
    List<Designacion> findByUsuarioId(Integer usuarioId);

    List<Designacion> findByDesignacionEstadoActualIgnoreCase(String designacionEstadoActual);

    boolean existsByUsuarioIdAndDesignacionNumeroResolucion(Integer usuarioId, String designacionNumeroResolucion);

    @Query("""
        SELECT d
        FROM Designacion d
        WHERE LOWER(d.designacionEstadoActual) IN ('activa', 'activo')
          AND d.designacionFechaFin IS NOT NULL
          AND d.designacionFechaFin >= :fechaDesde
          AND (CAST(:fechaHasta AS date) IS NULL OR d.designacionFechaFin <= :fechaHasta)
          AND (:unidadId IS NULL OR EXISTS (
              SELECT 1 FROM com.atlas.atlas_backend.cargo.Cargo c
              WHERE c.designacion = d AND c.unidad.unidadId = :unidadId
          ))
        ORDER BY d.designacionFechaFin ASC, d.designacionId ASC
    """)
    List<Designacion> findProximosVencimientos(
            @Param("unidadId") Integer unidadId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);

    @Query("""
        SELECT d
        FROM Designacion d
        WHERE LOWER(d.designacionEstadoActual) IN ('activa', 'activo')
          AND d.designacionFechaFin IS NOT NULL
          AND (CAST(:fechaHasta AS date) IS NULL OR d.designacionFechaFin <= :fechaHasta)
          AND (:unidadId IS NULL OR EXISTS (
              SELECT 1 FROM com.atlas.atlas_backend.cargo.Cargo c
              WHERE c.designacion = d AND c.unidad.unidadId = :unidadId
          ))
        ORDER BY d.designacionFechaFin ASC, d.designacionId ASC
    """)
    List<Designacion> findVencimientosIncluyendoVencidas(
            @Param("unidadId") Integer unidadId,
            @Param("fechaHasta") LocalDate fechaHasta);

    @Query("""
        SELECT COUNT(d)
        FROM Designacion d
        WHERE LOWER(d.designacionEstadoActual) IN ('activa', 'activo')
          AND d.designacionFechaFin IS NOT NULL
          AND d.designacionFechaFin >= :fechaDesde
          AND d.designacionFechaFin <= :fechaHasta
          AND (:unidadId IS NULL OR EXISTS (
              SELECT 1 FROM com.atlas.atlas_backend.cargo.Cargo c
              WHERE c.designacion = d AND c.unidad.unidadId = :unidadId
          ))
    """)
    long countVencimientos(
            @Param("unidadId") Integer unidadId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);
}


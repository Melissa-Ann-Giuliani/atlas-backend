package com.atlas.atlas_backend.dashboard;

import com.atlas.atlas_backend.cargo.Cargo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardRepository extends Repository<Cargo, Integer> {

    @Query("SELECT COUNT(DISTINCT d.usuarioId) " +
           "FROM Cargo c JOIN c.designacion d JOIN c.unidad u " +
           "WHERE (:unidadId IS NULL OR u.unidadId = :unidadId) " +
           "AND (:caracterId IS NULL OR c.caracter.caracterId = :caracterId) " +
           "AND (:categoriaId IS NULL OR c.categoria.categoriaId = :categoriaId) " +
           "AND (:dedicacionId IS NULL OR c.dedicacion.dedicacionId = :dedicacionId) " +
           "AND (:origenId IS NULL OR c.origen.origenId = :origenId) " +
           "AND (:estadoActual IS NULL OR d.designacionEstadoActual = :estadoActual)")
    long countTotalTeachers(@Param("unidadId") Integer unidadId, @Param("caracterId") Integer caracterId, @Param("categoriaId") Integer categoriaId, @Param("dedicacionId") Integer dedicacionId, @Param("origenId") Integer origenId, @Param("estadoActual") String estadoActual);

    @Query("SELECT tu.nombre, COUNT(DISTINCT d.usuarioId) " +
           "FROM Cargo c JOIN c.designacion d JOIN c.unidad u JOIN u.tipoUnidad tu " +
           "WHERE (:unidadId IS NULL OR u.unidadId = :unidadId) " +
           "AND (:caracterId IS NULL OR c.caracter.caracterId = :caracterId) " +
           "AND (:categoriaId IS NULL OR c.categoria.categoriaId = :categoriaId) " +
           "AND (:dedicacionId IS NULL OR c.dedicacion.dedicacionId = :dedicacionId) " +
           "AND (:origenId IS NULL OR c.origen.origenId = :origenId) " +
           "AND (:estadoActual IS NULL OR d.designacionEstadoActual = :estadoActual) " +
           "GROUP BY tu.nombre")
    List<Object[]> countTeachersByUnitType(@Param("unidadId") Integer unidadId, @Param("caracterId") Integer caracterId, @Param("categoriaId") Integer categoriaId, @Param("dedicacionId") Integer dedicacionId, @Param("origenId") Integer origenId, @Param("estadoActual") String estadoActual);

    @Query("SELECT tu.nombre, u.unidadNombre, COUNT(DISTINCT d.usuarioId) " +
           "FROM Cargo c JOIN c.designacion d JOIN c.unidad u JOIN u.tipoUnidad tu " +
           "WHERE (:unidadId IS NULL OR u.unidadId = :unidadId) " +
           "AND (:caracterId IS NULL OR c.caracter.caracterId = :caracterId) " +
           "AND (:categoriaId IS NULL OR c.categoria.categoriaId = :categoriaId) " +
           "AND (:dedicacionId IS NULL OR c.dedicacion.dedicacionId = :dedicacionId) " +
           "AND (:origenId IS NULL OR c.origen.origenId = :origenId) " +
           "AND (:estadoActual IS NULL OR d.designacionEstadoActual = :estadoActual) " +
           "GROUP BY tu.nombre, u.unidadNombre")
    List<Object[]> countTeachersByUnit(@Param("unidadId") Integer unidadId, @Param("caracterId") Integer caracterId, @Param("categoriaId") Integer categoriaId, @Param("dedicacionId") Integer dedicacionId, @Param("origenId") Integer origenId, @Param("estadoActual") String estadoActual);
}

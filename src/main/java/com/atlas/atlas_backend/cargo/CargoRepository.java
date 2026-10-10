package com.atlas.atlas_backend.cargo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Integer> {
    List<Cargo> findByDesignacionDesignacionIdIn(List<Integer> designacionIds);

    List<Cargo> findByCargoPredecesorId(Integer cargoPredecesorId);

    @Query("""
        SELECT c.id AS id, c.codigo AS numero, c.fechaCreacion AS fechaCreacion,
               c.estado AS estado,
               CASE WHEN c.estado = 'Asignado' THEN CONCAT(doc.apellido, ', ', doc.nombre) ELSE NULL END AS apellidoNombre,
               cat.categoriaNombre AS categoria, ded.dedicacionNombre AS dedicacion,
               car.caracterNombre AS caracter
        FROM Cargo c
        LEFT JOIN c.designacion des
        LEFT JOIN Docente doc ON doc.id = des.usuarioId
        LEFT JOIN c.categoria cat
        LEFT JOIN c.dedicacion ded
        LEFT JOIN c.caracter car
        JOIN c.unidad u
        WHERE (:estado IS NULL OR c.estado = :estado)
          AND (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (:categoriaId IS NULL OR cat.categoriaId = :categoriaId)
          AND (:dedicacionId IS NULL OR ded.dedicacionId = :dedicacionId)
          AND (:caracterId IS NULL OR car.caracterId = :caracterId)
          AND (:searchTerm IS NULL OR LOWER(CAST(c.codigo AS String)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR (c.estado = 'Asignado' AND LOWER(doc.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) 
               OR (c.estado = 'Asignado' AND LOWER(doc.apellido) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) 
               OR LOWER(cat.categoriaNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(ded.dedicacionNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(car.caracterNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<CargoListDTO> findActiveCargos(
            @Param("unidadId") Integer unidadId,
            @Param("searchTerm") String searchTerm,
            @Param("estado") String estado,
            @Param("categoriaId") Integer categoriaId,
            @Param("dedicacionId") Integer dedicacionId,
            @Param("caracterId") Integer caracterId,
            Pageable pageable);
}

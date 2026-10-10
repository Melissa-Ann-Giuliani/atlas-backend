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
               CONCAT(doc.apellido, ' ', doc.nombre) AS apellidoNombre,
               cat.categoriaNombre AS categoria, ded.dedicacionNombre AS dedicacion,
               car.caracterNombre AS caracter
        FROM Cargo c
        JOIN c.designacion des
        JOIN Docente doc ON doc.id = des.usuarioId
        JOIN c.categoria cat
        JOIN c.dedicacion ded
        JOIN c.caracter car
        JOIN c.unidad u
        WHERE c.estado = 'Asignado'
          AND (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (:searchTerm IS NULL OR LOWER(CAST(c.codigo AS String)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(doc.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR LOWER(doc.apellido) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR LOWER(cat.categoriaNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(ded.dedicacionNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(car.caracterNombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<CargoListDTO> findActiveCargos(
            @Param("unidadId") Integer unidadId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}

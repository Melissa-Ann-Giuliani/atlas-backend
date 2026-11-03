package com.atlas.atlas_backend.materia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Integer> {
    boolean existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio(String nombre, Integer unidadId, Integer planEstudio);
    boolean existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(String nombre, Integer unidadId, Integer planEstudio, Integer id);

    @Query("""
        SELECT m FROM Materia m
        JOIN FETCH m.unidad u
        LEFT JOIN FETCH u.tipoUnidad tu
        WHERE (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (CAST(:search AS string) IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY m.nombre ASC
    """)
    Page<Materia> searchMaterias(
            @Param("unidadId") Integer unidadId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM Materia m
        JOIN FETCH m.unidad u
        LEFT JOIN FETCH u.tipoUnidad tu
        WHERE (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (CAST(:search AS string) IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY m.nombre ASC
    """)
    List<Materia> searchMateriasList(
            @Param("unidadId") Integer unidadId,
            @Param("search") String search
    );
}


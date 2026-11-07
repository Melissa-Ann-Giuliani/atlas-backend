package com.atlas.atlas_backend.catedra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatedraRepository extends JpaRepository<Catedra, Integer> {
    boolean existsByMateria_IdAndEstadoIgnoreCase(Integer materiaId, String estado);
    boolean existsByNombreIgnoreCaseAndMateria_Id(String nombre, Integer materiaId);
    boolean existsByNombreIgnoreCaseAndMateria_IdAndIdNot(String nombre, Integer materiaId, Integer id);

    @Query("""
        SELECT c FROM Catedra c
        JOIN FETCH c.materia m
        JOIN FETCH m.unidad u
        LEFT JOIN FETCH u.tipoUnidad tu
        JOIN FETCH c.director d
        WHERE (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (CAST(:search AS string) IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY c.nombre ASC
    """)
    Page<Catedra> searchCatedras(
            @Param("unidadId") Integer unidadId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Catedra c
        JOIN FETCH c.materia m
        JOIN FETCH m.unidad u
        LEFT JOIN FETCH u.tipoUnidad tu
        JOIN FETCH c.director d
        WHERE (:unidadId IS NULL OR u.unidadId = :unidadId)
          AND (CAST(:search AS string) IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY c.nombre ASC
    """)
    List<Catedra> searchCatedrasList(
            @Param("unidadId") Integer unidadId,
            @Param("search") String search
    );
}
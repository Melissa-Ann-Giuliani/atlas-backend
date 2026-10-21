package com.atlas.atlas_backend.materia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Integer> {
    boolean existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio(String nombre, Integer unidadId, Integer planEstudio);
    boolean existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(String nombre, Integer unidadId, Integer planEstudio, Integer id);
}

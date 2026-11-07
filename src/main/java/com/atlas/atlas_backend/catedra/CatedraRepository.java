package com.atlas.atlas_backend.catedra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatedraRepository extends JpaRepository<Catedra, Integer> {
    boolean existsByMateria_IdAndEstadoIgnoreCase(Integer materiaId, String estado);
    boolean existsByNombreIgnoreCaseAndMateria_Id(String nombre, Integer materiaId);
    boolean existsByNombreIgnoreCaseAndMateria_IdAndIdNot(String nombre, Integer materiaId, Integer id);
}

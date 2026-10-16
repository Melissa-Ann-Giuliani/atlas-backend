package com.atlas.atlas_backend.unidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UnidadRepository extends JpaRepository<Unidad, Integer> {
    List<Unidad> findByTipoUnidad_Id(Integer tipoUnidadId);
    List<Unidad> findByTipoUnidad_NombreIgnoreCase(String tipoUnidadNombre);
}

package com.atlas.atlas_backend.unidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoUnidadRepository extends JpaRepository<TipoUnidad, Integer> {
}

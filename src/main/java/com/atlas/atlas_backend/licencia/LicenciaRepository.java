package com.atlas.atlas_backend.licencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Integer> {
    java.util.List<Licencia> findByDesignacionDesignacionIdIn(java.util.List<Integer> designacionIds);
}

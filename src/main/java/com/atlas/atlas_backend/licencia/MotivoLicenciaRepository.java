package com.atlas.atlas_backend.licencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotivoLicenciaRepository extends JpaRepository<MotivoLicencia, Integer> {
}

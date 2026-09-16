package com.atlas.atlas_backend.renuncia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotivoRenunciaRepository extends JpaRepository<MotivoRenuncia, Integer> {
}

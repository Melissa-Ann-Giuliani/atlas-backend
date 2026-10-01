package com.atlas.atlas_backend.designacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignacionRepository extends JpaRepository<Designacion, Integer> {
    List<Designacion> findByUsuarioId(Integer usuarioId);
}

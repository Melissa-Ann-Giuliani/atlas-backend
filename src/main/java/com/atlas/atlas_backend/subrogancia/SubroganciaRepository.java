package com.atlas.atlas_backend.subrogancia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubroganciaRepository extends JpaRepository<Subrogancia, Integer> {
}

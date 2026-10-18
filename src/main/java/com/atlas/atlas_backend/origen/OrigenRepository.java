package com.atlas.atlas_backend.origen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrigenRepository extends JpaRepository<Origen, Integer> {
}

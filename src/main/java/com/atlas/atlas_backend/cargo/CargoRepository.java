package com.atlas.atlas_backend.cargo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Integer> {
    java.util.List<Cargo> findByDesignacionDesignacionIdIn(java.util.List<Integer> designacionIds);
}

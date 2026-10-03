package com.atlas.atlas_backend.dedicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DedicacionRepository extends JpaRepository<Dedicacion, Integer> {
}

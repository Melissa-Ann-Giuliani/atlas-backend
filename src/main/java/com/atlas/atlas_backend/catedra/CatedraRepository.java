package com.atlas.atlas_backend.catedra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatedraRepository extends JpaRepository<Catedra, Integer> {
}

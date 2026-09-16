package com.atlas.atlas_backend.caracter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaracterRepository extends JpaRepository<Caracter, Integer> {
}

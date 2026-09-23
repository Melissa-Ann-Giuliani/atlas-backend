package com.atlas.atlas_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    boolean existsByDni(Long dni);

    boolean existsByCuil(Long cuil);

    boolean existsByEmailInstitucional(String emailInstitucional);
}

package com.atlas.atlas_backend.cambio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface CambioRepository extends JpaRepository<Cambio, Integer>, JpaSpecificationExecutor<Cambio> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"usuario", "usuarioAfectado", "unidad"})
    Page<Cambio> findAll(@Nullable Specification<Cambio> spec, @NonNull Pageable pageable);
}

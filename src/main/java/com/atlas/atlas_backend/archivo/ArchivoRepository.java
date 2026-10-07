package com.atlas.atlas_backend.archivo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArchivoRepository extends JpaRepository<Archivo, Integer> {
    List<Archivo> findByTipoTramiteAndTramiteId(String tipoTramite, Integer tramiteId);
}

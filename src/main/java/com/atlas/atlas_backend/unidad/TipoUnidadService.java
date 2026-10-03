package com.atlas.atlas_backend.unidad;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TipoUnidadService {
    private final TipoUnidadRepository repository;

    public TipoUnidadService(TipoUnidadRepository repository) {
        this.repository = repository;
    }

    public List<TipoUnidad> findAll() {
        return repository.findAll();
    }
}

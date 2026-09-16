package com.atlas.atlas_backend.unidad;

import org.springframework.stereotype.Service;

@Service
public class TipoUnidadService {
    private final TipoUnidadRepository repository;

    public TipoUnidadService(TipoUnidadRepository repository) {
        this.repository = repository;
    }
}

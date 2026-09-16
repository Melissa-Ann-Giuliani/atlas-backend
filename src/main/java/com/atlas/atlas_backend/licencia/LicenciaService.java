package com.atlas.atlas_backend.licencia;

import org.springframework.stereotype.Service;

@Service
public class LicenciaService {
    private final LicenciaRepository repository;

    public LicenciaService(LicenciaRepository repository) {
        this.repository = repository;
    }
}

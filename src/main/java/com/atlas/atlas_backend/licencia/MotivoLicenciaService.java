package com.atlas.atlas_backend.licencia;

import org.springframework.stereotype.Service;

@Service
public class MotivoLicenciaService {
    private final MotivoLicenciaRepository repository;

    public MotivoLicenciaService(MotivoLicenciaRepository repository) {
        this.repository = repository;
    }
}

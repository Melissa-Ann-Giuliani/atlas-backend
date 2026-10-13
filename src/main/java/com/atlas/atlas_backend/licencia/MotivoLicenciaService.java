package com.atlas.atlas_backend.licencia;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MotivoLicenciaService {
    private final MotivoLicenciaRepository repository;

    public MotivoLicenciaService(MotivoLicenciaRepository repository) {
        this.repository = repository;
    }

    public List<MotivoLicencia> findAll() {
        return repository.findAll();
    }
}

package com.atlas.atlas_backend.renuncia;

import org.springframework.stereotype.Service;

@Service
public class MotivoRenunciaService {
    private final MotivoRenunciaRepository repository;

    public MotivoRenunciaService(MotivoRenunciaRepository repository) {
        this.repository = repository;
    }
}

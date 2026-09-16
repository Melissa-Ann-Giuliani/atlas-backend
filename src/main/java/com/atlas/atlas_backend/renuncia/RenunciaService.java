package com.atlas.atlas_backend.renuncia;

import org.springframework.stereotype.Service;

@Service
public class RenunciaService {
    private final RenunciaRepository repository;

    public RenunciaService(RenunciaRepository repository) {
        this.repository = repository;
    }
}

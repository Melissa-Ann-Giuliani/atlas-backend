package com.atlas.atlas_backend.cambio;

import org.springframework.stereotype.Service;

@Service
public class CambioService {
    private final CambioRepository repository;

    public CambioService(CambioRepository repository) {
        this.repository = repository;
    }
}

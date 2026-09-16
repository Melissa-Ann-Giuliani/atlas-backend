package com.atlas.atlas_backend.funcion;

import org.springframework.stereotype.Service;

@Service
public class FuncionService {
    private final FuncionRepository repository;

    public FuncionService(FuncionRepository repository) {
        this.repository = repository;
    }
}

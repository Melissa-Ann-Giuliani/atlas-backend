package com.atlas.atlas_backend.catedra;

import org.springframework.stereotype.Service;

@Service
public class CatedraService {
    private final CatedraRepository repository;

    public CatedraService(CatedraRepository repository) {
        this.repository = repository;
    }
}

package com.atlas.atlas_backend.subrogancia;

import org.springframework.stereotype.Service;

@Service
public class SubroganciaService {
    private final SubroganciaRepository repository;

    public SubroganciaService(SubroganciaRepository repository) {
        this.repository = repository;
    }
}

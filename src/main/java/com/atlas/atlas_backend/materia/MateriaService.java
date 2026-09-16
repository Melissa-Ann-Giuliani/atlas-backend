package com.atlas.atlas_backend.materia;

import org.springframework.stereotype.Service;

@Service
public class MateriaService {
    private final MateriaRepository repository;

    public MateriaService(MateriaRepository repository) {
        this.repository = repository;
    }
}

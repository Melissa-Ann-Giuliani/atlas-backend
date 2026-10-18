package com.atlas.atlas_backend.origen;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrigenService {
    private final OrigenRepository repository;

    public OrigenService(OrigenRepository repository) {
        this.repository = repository;
    }

    public List<Origen> findAll() {
        return repository.findAll();
    }
}

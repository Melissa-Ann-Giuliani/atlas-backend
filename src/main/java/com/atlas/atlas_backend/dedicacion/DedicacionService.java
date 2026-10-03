package com.atlas.atlas_backend.dedicacion;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DedicacionService {
    private final DedicacionRepository repository;

    public DedicacionService(DedicacionRepository repository) {
        this.repository = repository;
    }

    public List<Dedicacion> findAll() {
        return repository.findAll();
    }
}

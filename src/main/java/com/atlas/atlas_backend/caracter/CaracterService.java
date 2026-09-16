package com.atlas.atlas_backend.caracter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CaracterService {

    @Autowired
    private CaracterRepository caracterRepository;

    public List<Caracter> findAll() {
        return caracterRepository.findAll();
    }

    public Optional<Caracter> findById(@NonNull Integer id) {
        return caracterRepository.findById(id);
    }

    public Caracter save(@NonNull Caracter caracter) {
        return caracterRepository.save(caracter);
    }

    public void deleteById(@NonNull Integer id) {
        caracterRepository.deleteById(id);
    }
}

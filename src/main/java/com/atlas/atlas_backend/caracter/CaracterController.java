package com.atlas.atlas_backend.caracter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caracteres")
public class CaracterController {

    @Autowired
    private CaracterService caracterService;

    @GetMapping
    public List<Caracter> getAllCaracteres() {
        return caracterService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Caracter> getCaracterById(@PathVariable @NonNull Integer id) {
        return caracterService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Caracter createCaracter(@RequestBody @NonNull Caracter caracter) {
        return caracterService.save(caracter);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Caracter> updateCaracter(@PathVariable @NonNull Integer id,
            @RequestBody Caracter caracterDetails) {
        return caracterService.findById(id)
                .map(caracter -> {
                    caracter.setCaracterNombre(caracterDetails.getCaracterNombre());
                    return ResponseEntity.ok(caracterService.save(caracter));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCaracter(@PathVariable @NonNull Integer id) {
        return caracterService.findById(id)
                .map(caracter -> {
                    caracterService.deleteById(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

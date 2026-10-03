package com.atlas.atlas_backend.unidad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-unidad")
public class TipoUnidadController {

    @Autowired
    private TipoUnidadService tipoUnidadService;

    @GetMapping
    public List<TipoUnidad> getAllTiposUnidad() {
        return tipoUnidadService.findAll();
    }
}

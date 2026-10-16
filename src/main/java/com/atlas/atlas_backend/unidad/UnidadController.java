package com.atlas.atlas_backend.unidad;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadController {

    private final UnidadService unidadService;

    public UnidadController(UnidadService unidadService) {
        this.unidadService = unidadService;
    }

    @GetMapping
    public List<UnidadResponse> getUnidades(
            @RequestParam(required = false) Integer tipoUnidadId,
            @RequestParam(required = false) String tipo) {
        return unidadService.getUnidades(tipoUnidadId, tipo);
    }
}

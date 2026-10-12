package com.atlas.atlas_backend.licencia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/motivos-licencias")
public class MotivoLicenciaController {

    @Autowired
    private MotivoLicenciaService motivoLicenciaService;

    @GetMapping
    public List<MotivoLicencia> getAllMotivosLicencias() {
        return motivoLicenciaService.findAll();
    }
}

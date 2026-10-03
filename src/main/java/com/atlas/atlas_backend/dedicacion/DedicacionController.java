package com.atlas.atlas_backend.dedicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dedicaciones")
public class DedicacionController {

    @Autowired
    private DedicacionService dedicacionService;

    @GetMapping
    public List<Dedicacion> getAllDedicaciones() {
        return dedicacionService.findAll();
    }
}

package com.atlas.atlas_backend.origen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/origenes")
public class OrigenController {

    @Autowired
    private OrigenService origenService;

    @GetMapping
    public List<Origen> getAllOrigenes() {
        return origenService.findAll();
    }
}

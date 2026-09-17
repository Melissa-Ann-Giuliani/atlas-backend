package com.atlas.atlas_backend.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolRepository rolRepository; // adjust to your actual repository name

    @GetMapping
    public List<Rol> getAllRoles() { // adjust "Roles" to your actual entity class name
        return rolRepository.findAll();
    }
}

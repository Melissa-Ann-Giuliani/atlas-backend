package com.atlas.atlas_backend.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/docentes")
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<Page<DocenteListDTO>> getActiveTeachers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String dedicacion,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String caracter,
            @RequestParam(required = false) String tipoUnidad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocenteListDTO> result = docenteService.getActiveTeachers(
                    authentication.getName(), search, origen, dedicacion, categoria, caracter, tipoUnidad, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<DocenteDetailsDTO> getTeacherDetails(@PathVariable @NonNull Integer id) {
        try {
            DocenteDetailsDTO dto = docenteService.getTeacherProfile(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

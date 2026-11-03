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

import com.atlas.atlas_backend.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/docentes")
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<Page<DocenteListDTO>> getActiveTeachers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String dedicacion,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String caracter,
            @RequestParam(required = false) String tipoUnidad,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            search = (search != null && search.trim().isEmpty()) ? null : search;
            origen = (origen != null && origen.trim().isEmpty()) ? null : origen;
            dedicacion = (dedicacion != null && dedicacion.trim().isEmpty()) ? null : dedicacion;
            categoria = (categoria != null && categoria.trim().isEmpty()) ? null : categoria;
            caracter = (caracter != null && caracter.trim().isEmpty()) ? null : caracter;
            tipoUnidad = (tipoUnidad != null && tipoUnidad.trim().isEmpty()) ? null : tipoUnidad;
            estado = (estado != null && estado.trim().isEmpty()) ? null : estado;

            Pageable pageable = PageRequest.of(page, size);
            Page<DocenteListDTO> result = docenteService.getActiveTeachers(
                    authentication.getName(), search, origen, dedicacion, categoria, caracter, tipoUnidad, estado, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<DocenteDetailsDTO> getTeacherDetails(@PathVariable @NonNull Integer id) {
        try {
            DocenteDetailsDTO dto = docenteService.getTeacherProfile(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> modificarPerfilDocente(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody DocenteProfileUpdateRequest request,
            Authentication authentication) {
        try {
            DocenteDetailsDTO dto = docenteService.modificarPerfilDocente(id, request, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{docenteId}/cargos/{cargoId}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> modificarCargoDocente(
            @PathVariable @NonNull Integer docenteId,
            @PathVariable @NonNull Integer cargoId,
            @Valid @RequestBody DocenteCargoUpdateRequest request,
            Authentication authentication) {
        try {
            DocenteDetailsDTO dto = docenteService.modificarCargoDocente(docenteId, cargoId, request, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{docenteId}/funciones/{funcionId}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> modificarFuncionDocente(
            @PathVariable @NonNull Integer docenteId,
            @PathVariable @NonNull Integer funcionId,
            @Valid @RequestBody DocenteFuncionUpdateRequest request,
            Authentication authentication) {
        try {
            DocenteDetailsDTO dto = docenteService.modificarFuncionDocente(docenteId, funcionId, request, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }
}


package com.atlas.atlas_backend.materia;

import com.atlas.atlas_backend.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/materias")
@CrossOrigin(origins = "*")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> crearMateria(
            @Valid @RequestBody @NonNull MateriaCreateRequest request,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            MateriaDetailDTO dto = materiaService.crearMateria(request, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> modificarMateria(
            @PathVariable("id") @NonNull Integer id,
            @Valid @RequestBody @NonNull MateriaPatchRequest request,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            MateriaDetailDTO dto = materiaService.modificarMateria(id, request, username);
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

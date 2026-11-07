package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.catedra.dto.CatedraRequestDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraResponseDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraUpdateRequestDTO;
import com.atlas.atlas_backend.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/catedras")
@CrossOrigin(origins = "*")
public class CatedraController {

    private final CatedraService catedraService;

    public CatedraController(CatedraService catedraService) {
        this.catedraService = catedraService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> crearCatedra(
            @Valid @RequestBody @NonNull CatedraRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            CatedraResponseDTO dto = catedraService.crearCatedra(request, username);
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> modificarCatedra(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody @NonNull CatedraUpdateRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            CatedraResponseDTO dto = catedraService.modificarCatedra(id, request, username);
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

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> listarCatedras(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            if (page != null || size != null) {
                int pageNumber = page != null ? page : 0;
                int pageSize = size != null ? size : 50;
                if (pageNumber < 0 || pageSize < 1 || pageSize > 500) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponse("Parámetros de paginación inválidos"));
                }
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                Page<CatedraResponseDTO> result = catedraService.listarCatedras(search, pageable, username);
                return ResponseEntity.ok(result);
            }
            List<CatedraResponseDTO> list = catedraService.listarCatedras(search, username);
            return ResponseEntity.ok(list);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> obtenerCatedraPorId(
            @PathVariable @NonNull Integer id,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            CatedraResponseDTO dto = catedraService.obtenerCatedraPorId(id, username);
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
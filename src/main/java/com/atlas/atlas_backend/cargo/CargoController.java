package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/cargos")
public class CargoController {

    @Autowired
    private CargoService cargoService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<Page<CargoListDTO>> getActiveCargos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer dedicacionId,
            @RequestParam(required = false) Integer caracterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            String safeSearch = (search == null) ? "" : search;
            Page<CargoListDTO> result = cargoService.getActiveCargos(
                    authentication.getName(), safeSearch, estado, categoriaId, dedicacionId, caracterId, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().header("X-Error-Message", e.getMessage()).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<CargoDetailDTO> getCargoDetails(@PathVariable @NonNull Integer id,
            Authentication authentication) {
        try {
            CargoDetailDTO dto = cargoService.getCargoDetails(id, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> crearCargo(@Valid @RequestBody CargoCreateRequest request, Authentication authentication) {
        try {
            CargoDetailDTO dto = cargoService.crearCargo(request, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> modificarCargo(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody CargoUpdateRequest request,
            Authentication authentication) {
        try {
            CargoDetailDTO dto = cargoService.modificarCargo(id, request, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/subdividir")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    public ResponseEntity<?> subdividirCargo(
            @Valid @RequestBody SubdividirCargoRequest request,
            Authentication authentication) {
        try {
            java.util.List<CargoDetailDTO> dtos = cargoService.subdividirCargo(request, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }
}


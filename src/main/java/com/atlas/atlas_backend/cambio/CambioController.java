package com.atlas.atlas_backend.cambio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cambios")
public class CambioController {

    private final CambioService cambioService;

    public CambioController(CambioService cambioService) {
        this.cambioService = cambioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_UNIDAD', 'AUDITOR')")
    public ResponseEntity<Page<CambioResponseDTO>> consultarHistorial(
            @RequestParam(required = false) String usuarioAfectado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) TipoOperacion tipoOperacion,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer unidadId,
            @PageableDefault(size = 20) @NonNull Pageable pageable) {

        Page<CambioResponseDTO> result = cambioService.consultarHistorial(
                usuarioAfectado, fechaDesde, fechaHasta, tipoOperacion, usuarioId, categoria, unidadId, pageable);
        return ResponseEntity.ok(result);
    }
}

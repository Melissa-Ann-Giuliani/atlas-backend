package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.error.ErrorResponse;
import com.atlas.atlas_backend.export.ExportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/designaciones")
public class DesignacionController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private DesignacionService designacionService;

    @Autowired
    private ExportService exportService;

    @GetMapping("/vencimientos")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> obtenerVencimientos(
            @RequestParam(required = false, defaultValue = "false") Boolean incluirVencidas,
            @RequestParam(required = false) Integer unidadId,
            @RequestParam(required = false) Integer dias,
            @RequestParam(required = false, defaultValue = "false") Boolean sinLimite,
            @RequestParam(required = false) List<String> filtros,
            Authentication authentication) {
        try {
            Integer effectiveDias = Boolean.TRUE.equals(sinLimite) ? Integer.valueOf(0) : dias;
            List<DesignacionVencimientoDTO> dtos = designacionService.obtenerVencimientos(
                    unidadId, incluirVencidas, effectiveDias, authentication.getName());
            return ResponseEntity.ok(dtos);
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

    @GetMapping("/vencimientos/exportar")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> exportarVencimientos(
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false, defaultValue = "false") Boolean incluirVencidas,
            @RequestParam(required = false) Integer unidadId,
            @RequestParam(required = false) Integer dias,
            @RequestParam(required = false, defaultValue = "false") Boolean sinLimite,
            @RequestParam(required = false) List<String> filtros,
            Authentication authentication) {
        try {
            Integer effectiveDias = Boolean.TRUE.equals(sinLimite) ? Integer.valueOf(0) : dias;
            List<DesignacionVencimientoDTO> dtos = designacionService.obtenerVencimientos(
                    unidadId, incluirVencidas, effectiveDias, authentication.getName());

            List<String> headers = List.of(
                    "Nro. Resolución", "Docente", "DNI", "Legajo", "Unidad",
                    "Fecha Inicio", "Fecha Fin", "Días Restantes", "Estado");

            List<List<String>> rows = dtos.stream()
                    .map(d -> {
                        String docente = "";
                        String dni = "";
                        String legajo = "";
                        if (d.getDocente() != null) {
                            String ap = d.getDocente().getApellido() != null ? d.getDocente().getApellido() : "";
                            String nom = d.getDocente().getNombre() != null ? d.getDocente().getNombre() : "";
                            docente = (ap + " " + nom).trim();
                            dni = d.getDocente().getDocumento() != null ? d.getDocente().getDocumento() : "";
                            legajo = d.getDocente().getLegajo() != null ? d.getDocente().getLegajo() : "";
                        }
                        String unidad = (d.getUnidad() != null && d.getUnidad().getNombre() != null)
                                ? d.getUnidad().getNombre() : "";
                        return List.of(
                                d.getNumeroResolucion() != null ? d.getNumeroResolucion() : "",
                                docente,
                                dni,
                                legajo,
                                unidad,
                                d.getFechaInicio() != null ? d.getFechaInicio().format(DATE_FMT) : "",
                                d.getFechaFin() != null ? d.getFechaFin().format(DATE_FMT) : "",
                                d.getDiasRestantes() != null ? String.valueOf(d.getDiasRestantes()) : "",
                                d.getEstadoActual() != null ? d.getEstadoActual() : "");
                    })
                    .toList();

            return exportService.buildExportResponse(
                    "vencimientos_designaciones", "Vencimientos de Designaciones", headers, rows, formato, filtros);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(new ErrorResponse(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/vencimientos/resumen")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> obtenerResumenVencimientos(
            @RequestParam(required = false) Integer unidadId,
            Authentication authentication) {
        try {
            DesignacionVencimientosResumenDTO dto = designacionService.obtenerResumenVencimientos(
                    unidadId, authentication.getName());
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> crearDesignacion(
            @Valid @RequestBody DesignacionCreateRequest request,
            Authentication authentication) {
        try {
            DesignacionResponseDTO dto = designacionService.registrarDesignacion(request, authentication.getName());
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

    @PostMapping("/{id}/redesignar")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> redesignar(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody DesignacionRedesignarRequest request,
            Authentication authentication) {
        try {
            DesignacionResponseDTO dto = designacionService.redesignarDocente(id, request, authentication.getName());
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

    @PatchMapping("/{id}/cargo")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> vincularCargo(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody DesignacionVincularCargoRequest request,
            Authentication authentication) {
        try {
            DesignacionResponseDTO dto = designacionService.vincularCargo(id, request.getCargoId(), authentication.getName());
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")
    public ResponseEntity<?> obtenerDesignacionPorId(@PathVariable @NonNull Integer id) {
        try {
            DesignacionResponseDTO dto = designacionService.obtenerPorId(id);
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
}


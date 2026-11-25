package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.error.ErrorResponse;
import com.atlas.atlas_backend.export.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/cambios")
public class CambioController {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CambioService cambioService;

    @Autowired
    private ExportService exportService;

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

    @GetMapping("/exportar")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_UNIDAD', 'AUDITOR')")
    public ResponseEntity<?> exportarHistorial(
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false) String usuarioAfectado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) TipoOperacion tipoOperacion,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer unidadId,
            @RequestParam(required = false) List<String> filtros) {
        try {
            Page<CambioResponseDTO> result = cambioService.consultarHistorial(
                    usuarioAfectado, fechaDesde, fechaHasta, tipoOperacion, usuarioId, categoria, unidadId,
                    Pageable.unpaged());

            List<String> headers = List.of(
                    "Fecha y Hora", "Responsable", "Operación", "Categoría",
                    "Campo Afectado", "Valor Anterior", "Valor Nuevo", "Usuario Afectado");

            List<List<String>> rows = result.getContent().stream()
                    .map(c -> {
                        String responsable = "";
                        if (c.getUsuarioResponsable() != null) {
                            String nombre = c.getUsuarioResponsable().getNombre() != null
                                    ? c.getUsuarioResponsable().getNombre() : "";
                            String apellido = c.getUsuarioResponsable().getApellido() != null
                                    ? c.getUsuarioResponsable().getApellido() : "";
                            responsable = (apellido + " " + nombre).trim();
                            if (responsable.isEmpty() && c.getUsuarioResponsable().getUsername() != null) {
                                responsable = c.getUsuarioResponsable().getUsername();
                            }
                        }
                        return List.of(
                                c.getFechaHora() != null ? c.getFechaHora().format(DATETIME_FMT) : "",
                                responsable,
                                c.getTipoOperacion() != null ? c.getTipoOperacion() : "",
                                c.getCategoria() != null ? c.getCategoria() : "",
                                c.getCampoAfectado() != null ? c.getCampoAfectado() : "",
                                c.getValorAnterior() != null ? c.getValorAnterior() : "",
                                c.getValorNuevo() != null ? c.getValorNuevo() : "",
                                c.getUsuarioAfectadoNombreCompleto() != null ? c.getUsuarioAfectadoNombreCompleto() : "");
                    })
                    .toList();

            return exportService.buildExportResponse("historial_cambios", "Historial de Cambios", headers, rows, formato, filtros);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(e.getMessage()));
        }
    }
}

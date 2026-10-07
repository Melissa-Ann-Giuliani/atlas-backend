package com.atlas.atlas_backend.archivo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/archivos")
@CrossOrigin(origins = "*")
public class ArchivoController {

    private final ArchivoService archivoService;

    public ArchivoController(ArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    @PostMapping("/subir")
    public ResponseEntity<Archivo> subirArchivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipoTramite") String tipoTramite,
            @RequestParam("tramiteId") Integer tramiteId) {
        try {
            Archivo archivo = archivoService.guardarArchivo(file, tipoTramite, tramiteId);
            return new ResponseEntity<>(archivo, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tramite/{tipoTramite}/{tramiteId}")
    public ResponseEntity<List<Archivo>> obtenerArchivosDeTramite(
            @PathVariable String tipoTramite,
            @PathVariable Integer tramiteId) {
        List<Archivo> archivos = archivoService.obtenerArchivosPorTramite(tipoTramite, tramiteId);
        return new ResponseEntity<>(archivos, HttpStatus.OK);
    }

    @GetMapping("/descargar/{nombreEncriptado}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable String nombreEncriptado) {
        try {
            Path file = archivoService.cargarArchivo(nombreEncriptado);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

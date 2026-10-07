package com.atlas.atlas_backend.archivo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivoService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final ArchivoRepository archivoRepository;

    public ArchivoService(ArchivoRepository archivoRepository) {
        this.archivoRepository = archivoRepository;
    }

    public Archivo guardarArchivo(MultipartFile file, String tipoTramite, Integer tramiteId) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String nombreEncriptado = UUID.randomUUID().toString() + extension;
        
        Path filePath = uploadPath.resolve(nombreEncriptado);
        Files.copy(file.getInputStream(), filePath);

        Archivo archivo = new Archivo();
        archivo.setNombreOriginal(originalFilename);
        archivo.setNombreEncriptado(nombreEncriptado);
        archivo.setTipoTramite(tipoTramite);
        archivo.setTramiteId(tramiteId);
        archivo.setFechaSubida(LocalDateTime.now());
        archivo.setTamano(file.getSize());
        archivo.setTipoContenido(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

        return archivoRepository.save(archivo);
    }

    public List<Archivo> obtenerArchivosPorTramite(String tipoTramite, Integer tramiteId) {
        return archivoRepository.findByTipoTramiteAndTramiteId(tipoTramite, tramiteId);
    }
    
    public Path cargarArchivo(String nombreEncriptado) {
        return Paths.get(uploadDir).resolve(nombreEncriptado);
    }
}

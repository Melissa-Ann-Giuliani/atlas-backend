package com.atlas.atlas_backend.archivo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArchivoAdjuntoDTO {
    private Integer id;
    private String nombreOriginal;
    private String nombreEncriptado;
    private LocalDateTime fechaSubida;
    private Long tamano;
    private String tipoContenido;
    private String url;

    public static ArchivoAdjuntoDTO fromArchivo(Archivo archivo) {
        ArchivoAdjuntoDTO dto = new ArchivoAdjuntoDTO();
        dto.setId(archivo.getArchivoId());
        dto.setNombreOriginal(archivo.getNombreOriginal());
        dto.setNombreEncriptado(archivo.getNombreEncriptado());
        dto.setFechaSubida(archivo.getFechaSubida());
        dto.setTamano(archivo.getTamano());
        dto.setTipoContenido(archivo.getTipoContenido());
        if (archivo.getNombreEncriptado() != null) {
            dto.setUrl("/api/archivos/descargar/" + archivo.getNombreEncriptado());
        }
        return dto;
    }
}

package com.atlas.atlas_backend.licencia;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LicenciaDetailDTO {
    // Licencia Info
    private Integer id;
    private String nroResolucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaFinEstim;
    private Integer cantidadDias;
    private Integer motivoId;
    private String motivoNombre;
    private Integer motivoDiasCorresp;
    private String estado;

    // Docente Info
    private String docenteNombreCompleto;
    private Long docenteDni;
    private Long docenteCuil;
    private String docenteEmail;
    private List<String> docenteTelefonos;

    // Designacion Info
    private Integer designacionId;
    private Integer designacionNroResolucion;
    private LocalDate designacionFechaInicio;
    private LocalDate designacionFechaFin;
    private String designacionEstado;

    // Academic Unit Info
    private Integer unidadId;
    private String unidadNombre;

    // Linked Cargo Info
    private List<String> cargosVinculados;
    private List<CargoVinculadoData> cargosDetalle;

    // Attached Documentation
    private List<ArchivoAdjuntoData> documentacionAdjunta;

    @Data
    public static class CargoVinculadoData {
        private Integer id;
        private Long codigo;
        private String categoria;
        private String dedicacion;
        private String caracter;
        private String estado;
        private String unidadNombre;
    }

    @Data
    public static class ArchivoAdjuntoData {
        private Integer id;
        private String nombreOriginal;
        private String nombreEncriptado;
        private LocalDateTime fechaSubida;
        private Long tamano;
        private String tipoContenido;
    }
}

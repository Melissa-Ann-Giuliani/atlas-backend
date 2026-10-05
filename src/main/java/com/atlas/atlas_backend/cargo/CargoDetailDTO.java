package com.atlas.atlas_backend.cargo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CargoDetailDTO {
    // Header Data
    private Long numero;
    private String categoria;
    private String estado;

    // Administrative Data
    private String asignadoANombre;
    private Long asignadoADni;
    private Integer nroResolucionDesignacion;
    private LocalDate fechaCreacion;
    private String dedicacion;
    private String caracter;

    // Linked Designation
    private Integer designacionNroResolucion;
    private LocalDate designacionFechaInicio;
    private LocalDate designacionFechaFin;

    // Linked Activities
    private List<ActividadData> actividadesVinculadas;

    // Predecessors and Subcargos
    private CargoPredecesorData cargoPredecesor;
    private List<SubcargoData> subcargosActivos;

    // Attached Documentation
    private List<String> documentacionAdjunta;

    @Data
    public static class ActividadData {
        private String materiaProyecto;
        private String origen;
        private Integer horas;
        private String estado;
    }

    @Data
    public static class CargoPredecesorData {
        private Long numero;
        private String estado;
        private String categoria;
    }

    @Data
    public static class SubcargoData {
        private Long numero;
        private String estado;
        private String categoria;
    }
}

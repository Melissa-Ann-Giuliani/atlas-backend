package com.atlas.atlas_backend.designacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignacionVencimientoDTO {

    private Integer designacionId;
    private String numeroResolucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Long diasRestantes;
    private String estadoActual;
    private DocenteResumenDTO docente;
    private UnidadResumenDTO unidad;
    private List<CargoResumenDTO> cargos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocenteResumenDTO {
        private Integer docenteId;
        private String nombre;
        private String apellido;
        private String documento;
        private String legajo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnidadResumenDTO {
        private Integer unidadId;
        private String nombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoResumenDTO {
        private Integer cargoId;
        private Long codigo;
        private String categoria;
        private String dedicacion;
        private String caracter;
    }
}

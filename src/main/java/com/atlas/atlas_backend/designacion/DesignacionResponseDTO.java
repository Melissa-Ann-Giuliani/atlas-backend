package com.atlas.atlas_backend.designacion;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DesignacionResponseDTO {

    private Integer id;
    private String nroResolucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private DocenteResumenDTO docente;
    private CargoResumenDTO cargo;

    @Data
    public static class DocenteResumenDTO {
        private Integer id;
        private Integer legajo;
        private String nombre;
        private String apellido;
        private Long dni;
        private String email;
    }

    @Data
    public static class CargoResumenDTO {
        private Integer id;
        private Long codigo;
        private String categoria;
        private String dedicacion;
        private String unidad;
    }
}


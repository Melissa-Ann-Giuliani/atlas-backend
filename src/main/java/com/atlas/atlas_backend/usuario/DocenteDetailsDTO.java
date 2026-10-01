package com.atlas.atlas_backend.usuario;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DocenteDetailsDTO {
    private PersonalData datosPersonales;
    private List<DesignacionData> designacionesYCargos;
    private List<ActividadData> actividadesActuales;
    private List<LicenciaData> licenciasActuales;

    @Data
    public static class PersonalData {
        private Long dni;
        private String emailInstitucional;
        private List<String> telefonos;
        private String nombre;
        private String apellido;
    }

    @Data
    public static class DesignacionData {
        private Integer legajo;
        private Integer nroResolucion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Long nroCargo;
        private String categoria;
        private String dedicacion;
        private String caracter;
        private String estado;
    }

    @Data
    public static class ActividadData {
        private String materiaProyecto;
        private String origen;
        private Integer horas;
        private String estado;
    }

    @Data
    public static class LicenciaData {
        private String nroResolucion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String motivo;
        private Integer cantidadDias;
    }
}

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
        private String estado;
        private String unidadNombre;
        private String tipoUnidadNombre;
    }

    @Data
    public static class DesignacionData {
        private Integer legajo;
        private Integer nroResolucion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Integer cargoId;
        private Long nroCargo;
        private String categoria;
        private String dedicacion;
        private String caracter;
        private String estado;
        private String origen;
    }

    @Data
    public static class ActividadData {
        private Integer funcionId;
        private String funcionNombre;
        private Integer funcionHoras;
        private Integer cargoId;
        private Integer materiaId;
        private String materiaNombre;
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

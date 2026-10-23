package com.atlas.atlas_backend.materia;

import lombok.Data;

@Data
public class MateriaDetailDTO {

    public MateriaDetailDTO() {
    }
    private Integer id;
    private String nombre;
    private Integer horas;
    private String anio;
    private String despliegue;
    private Integer planEstudio;
    private String estado;
    private Integer unidadId;
    private String unidadNombre;
    private Integer dependenciaId;
    private String dependenciaNombre;
}

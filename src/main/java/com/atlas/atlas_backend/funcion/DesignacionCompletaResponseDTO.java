package com.atlas.atlas_backend.funcion;

import lombok.Data;

import java.util.List;

@Data
public class DesignacionCompletaResponseDTO {

    private Integer cargoId;
    private Integer horasDedicacion;
    private Integer horasOcupadas;
    private List<FuncionDTO> funciones;
    private Integer designacionId;
    private Integer documentoId;
    private String docenteEstado;
    private Boolean notificacionEnviada;

    @Data
    public static class FuncionDTO {
        private Integer id;
        private String nombre;
        private String tipo;
        private Integer horas;
        private MateriaResumenDTO materia;
    }

    @Data
    public static class MateriaResumenDTO {
        private Integer id;
        private String nombre;
    }
}


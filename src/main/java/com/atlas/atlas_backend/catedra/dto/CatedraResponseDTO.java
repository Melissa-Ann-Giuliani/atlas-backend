package com.atlas.atlas_backend.catedra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatedraResponseDTO {
    private Integer id;
    private String nombre;
    private Integer tipologia;
    private String estado;
    private Integer directorId;
    private String directorNombre;
    private Integer materiaId;
    private String materiaNombre;
    private Integer unidadId;
    private String unidadNombre;
    private Integer tipoUnidadId;
    private String tipoUnidadNombre;
}
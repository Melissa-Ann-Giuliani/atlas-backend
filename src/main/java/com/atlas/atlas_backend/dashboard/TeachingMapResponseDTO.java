package com.atlas.atlas_backend.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
public class TeachingMapResponseDTO {
    private Long totalDocentes;
    private List<UnitTypeStatsDTO> unitTypes;
    private List<SpecificUnitStatsDTO> centros;
    private List<SpecificUnitStatsDTO> departamentos;
    private List<SpecificUnitStatsDTO> institutos;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SpecificUnitStatsDTO> materias;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String unidadNombre;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tipoUnidadNombre;
}

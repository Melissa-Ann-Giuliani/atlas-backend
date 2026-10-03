package com.atlas.atlas_backend.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class TeachingMapResponseDTO {
    private Long totalDocentes;
    private List<UnitTypeStatsDTO> unitTypes;
    private List<SpecificUnitStatsDTO> centros;
    private List<SpecificUnitStatsDTO> departamentos;
    private List<SpecificUnitStatsDTO> institutos;
}

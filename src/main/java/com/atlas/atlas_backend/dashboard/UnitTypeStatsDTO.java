package com.atlas.atlas_backend.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class UnitTypeStatsDTO {
    private String unitTypeName;
    private Long count;
    private List<SpecificUnitStatsDTO> specificUnits;
}

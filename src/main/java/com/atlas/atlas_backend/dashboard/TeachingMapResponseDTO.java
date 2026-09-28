package com.atlas.atlas_backend.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class TeachingMapResponseDTO {
    private Long totalTeachers;
    private List<UnitTypeStatsDTO> unitTypes;
}

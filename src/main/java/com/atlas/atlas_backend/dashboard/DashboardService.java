package com.atlas.atlas_backend.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    public TeachingMapResponseDTO getTeachingMapData(Integer unidadId, Integer caracterId, Integer categoriaId, Integer dedicacionId, Integer origenId, String estadoActual) {
        TeachingMapResponseDTO dto = new TeachingMapResponseDTO();
        dto.setTotalDocentes(dashboardRepository.countTotalTeachers(unidadId, caracterId, categoriaId, dedicacionId, origenId, estadoActual));

        List<Object[]> byUnitTypeData = dashboardRepository.countTeachersByUnitType(unidadId, caracterId, categoriaId, dedicacionId, origenId, estadoActual);
        Map<String, Long> byUnitTypeMap = new HashMap<>();
        byUnitTypeMap.put("Centro", 0L);
        byUnitTypeMap.put("Departamento", 0L);
        byUnitTypeMap.put("Instituto", 0L);

        for (Object[] row : byUnitTypeData) {
            byUnitTypeMap.put((String) row[0], (Long) row[1]);
        }

        List<Object[]> byUnitData = dashboardRepository.countTeachersByUnit(unidadId, caracterId, categoriaId, dedicacionId, origenId, estadoActual);
        Map<String, List<SpecificUnitStatsDTO>> specificUnitsMap = new HashMap<>();
        specificUnitsMap.put("Centro", new ArrayList<>());
        specificUnitsMap.put("Departamento", new ArrayList<>());
        specificUnitsMap.put("Instituto", new ArrayList<>());

        for (Object[] row : byUnitData) {
            String unitType = (String) row[0];
            String unitName = (String) row[1];
            Long count = (Long) row[2];

            SpecificUnitStatsDTO specificUnit = new SpecificUnitStatsDTO();
            specificUnit.setLabel(unitName);
            specificUnit.setValue(count);

            specificUnitsMap.computeIfAbsent(unitType, k -> new ArrayList<>()).add(specificUnit);
        }

        List<String> allTypesToInclude = new ArrayList<>(List.of("Centro", "Departamento", "Instituto"));
        for (String typeName : specificUnitsMap.keySet()) {
            if (!allTypesToInclude.contains(typeName)) {
                allTypesToInclude.add(typeName);
            }
        }

        List<UnitTypeStatsDTO> unitTypes = new ArrayList<>();
        for (String typeName : allTypesToInclude) {
            UnitTypeStatsDTO unitTypeStats = new UnitTypeStatsDTO();
            unitTypeStats.setLabel(typeName);
            unitTypeStats.setValue(byUnitTypeMap.getOrDefault(typeName, 0L));
            unitTypes.add(unitTypeStats);
        }

        dto.setUnitTypes(unitTypes);
        dto.setCentros(specificUnitsMap.getOrDefault("Centro", new ArrayList<>()));
        dto.setDepartamentos(specificUnitsMap.getOrDefault("Departamento", new ArrayList<>()));
        dto.setInstitutos(specificUnitsMap.getOrDefault("Instituto", new ArrayList<>()));
        return dto;
    }
}

package com.atlas.atlas_backend.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    public TeachingMapResponseDTO getTeachingMapData(
            Integer unidadId,
            List<Integer> caracterIds,
            List<Integer> categoriaIds,
            List<Integer> dedicacionIds,
            List<Integer> origenIds,
            List<String> estados) {
        List<Integer> cleanCaracterIds = sanitizeIds(caracterIds);
        List<Integer> cleanCategoriaIds = sanitizeIds(categoriaIds);
        List<Integer> cleanDedicacionIds = sanitizeIds(dedicacionIds);
        List<Integer> cleanOrigenIds = sanitizeIds(origenIds);
        List<String> cleanEstados = sanitizeEstados(estados);

        TeachingMapResponseDTO dto = new TeachingMapResponseDTO();
        dto.setTotalDocentes(dashboardRepository.countTotalTeachers(
                unidadId, cleanCaracterIds, cleanCategoriaIds, cleanDedicacionIds, cleanOrigenIds, cleanEstados));

        List<Object[]> byUnitTypeData = dashboardRepository.countTeachersByUnitType(
                unidadId, cleanCaracterIds, cleanCategoriaIds, cleanDedicacionIds, cleanOrigenIds, cleanEstados);
        Map<String, Long> byUnitTypeMap = new HashMap<>();
        byUnitTypeMap.put("Centro", 0L);
        byUnitTypeMap.put("Departamento", 0L);
        byUnitTypeMap.put("Instituto", 0L);

        for (Object[] row : byUnitTypeData) {
            byUnitTypeMap.put((String) row[0], (Long) row[1]);
        }

        List<Object[]> byUnitData = dashboardRepository.countTeachersByUnit(
                unidadId, cleanCaracterIds, cleanCategoriaIds, cleanDedicacionIds, cleanOrigenIds, cleanEstados);
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

        if (unidadId != null) {
            List<Object[]> activeMaterias = dashboardRepository.findActiveMateriasByUnidad(unidadId);
            List<Object[]> byMateriaData = dashboardRepository.countTeachersByMateria(
                    unidadId, cleanCaracterIds, cleanCategoriaIds, cleanDedicacionIds, cleanOrigenIds, cleanEstados);

            Map<Integer, Long> materiaCounts = new HashMap<>();
            if (byMateriaData != null) {
                for (Object[] row : byMateriaData) {
                    materiaCounts.put((Integer) row[0], (Long) row[1]);
                }
            }

            List<SpecificUnitStatsDTO> materias = new ArrayList<>();
            if (activeMaterias != null) {
                for (Object[] row : activeMaterias) {
                    Integer materiaId = (Integer) row[0];
                    String materiaNombre = (String) row[1];
                    SpecificUnitStatsDTO item = new SpecificUnitStatsDTO();
                    item.setLabel(materiaNombre);
                    item.setValue(materiaCounts.getOrDefault(materiaId, 0L));
                    materias.add(item);
                }
            }
            dto.setMaterias(materias);

            Object[] unidadMeta = dashboardRepository.findUnidadMetadata(unidadId);
            if (unidadMeta != null) {
                dto.setUnidadNombre((String) unidadMeta[0]);
                dto.setTipoUnidadNombre((String) unidadMeta[1]);
            }
        }

        return dto;
    }

    private List<Integer> sanitizeIds(List<Integer> ids) {
        if (ids == null) {
            return null;
        }
        List<Integer> cleaned = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private List<String> sanitizeEstados(List<String> estados) {
        if (estados == null) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        for (String estado : estados) {
            if (estado != null) {
                String normalized = estado.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty() && !cleaned.contains(normalized)) {
                    cleaned.add(normalized);
                }
            }
        }
        return cleaned.isEmpty() ? null : cleaned;
    }
}

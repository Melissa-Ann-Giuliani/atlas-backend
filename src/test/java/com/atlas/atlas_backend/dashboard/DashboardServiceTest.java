package com.atlas.atlas_backend.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTeachingMapData_ReturnsProperlyFormattedDTO() {
        when(dashboardRepository.countTotalTeachers(null, null, null, null, null, null)).thenReturn(100L);

        List<Object[]> unitTypeData = new ArrayList<>();
        unitTypeData.add(new Object[]{"Centro", 10L});
        unitTypeData.add(new Object[]{"Departamento", 50L});
        unitTypeData.add(new Object[]{"Instituto", 40L});
        when(dashboardRepository.countTeachersByUnitType(null, null, null, null, null, null)).thenReturn(unitTypeData);

        List<Object[]> unitData = new ArrayList<>();
        unitData.add(new Object[]{"Departamento", "Matemática", 30L});
        unitData.add(new Object[]{"Departamento", "Música", 20L});
        unitData.add(new Object[]{"Centro", "Creación", 10L});
        when(dashboardRepository.countTeachersByUnit(null, null, null, null, null, null)).thenReturn(unitData);

        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(100L, result.getTotalDocentes());
        assertEquals(3, result.getUnitTypes().size());

        UnitTypeStatsDTO deptStats = result.getUnitTypes().stream()
                .filter(u -> u.getLabel().equals("Departamento"))
                .findFirst().orElse(null);
        assertNotNull(deptStats);
        assertEquals(50L, deptStats.getValue());
        assertEquals(2, result.getDepartamentos().size());

        SpecificUnitStatsDTO matStats = result.getDepartamentos().stream()
                .filter(u -> u.getLabel().equals("Matemática"))
                .findFirst().orElse(null);
        assertNotNull(matStats);
        assertEquals(30L, matStats.getValue());
    }

    @Test
    void testGetTeachingMapData_HandlesEmptyDatabase() {
        when(dashboardRepository.countTotalTeachers(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(dashboardRepository.countTeachersByUnitType(any(), any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(dashboardRepository.countTeachersByUnit(any(), any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());

        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(0L, result.getTotalDocentes());
        assertEquals(3, result.getUnitTypes().size());

        for (UnitTypeStatsDTO type : result.getUnitTypes()) {
            assertEquals(0L, type.getValue());
        }
        assertEquals(0, result.getCentros().size());
        assertEquals(0, result.getDepartamentos().size());
        assertEquals(0, result.getInstitutos().size());
    }

    @Test
    void testGetTeachingMapData_SanitizesFilterLists() {
        List<Integer> caracterIds = List.of(1, 2);
        List<Integer> origenIds = List.of(3);
        List<String> rawEstados = List.of(" Activo ", " ", "Licencia");
        List<String> expectedEstados = List.of("activo", "licencia");

        when(dashboardRepository.countTotalTeachers(null, caracterIds, null, null, origenIds, expectedEstados))
                .thenReturn(5L);
        when(dashboardRepository.countTeachersByUnitType(null, caracterIds, null, null, origenIds, expectedEstados))
                .thenReturn(List.of());
        when(dashboardRepository.countTeachersByUnit(null, caracterIds, null, null, origenIds, expectedEstados))
                .thenReturn(List.of());

        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(
                null, caracterIds, List.of(), null, origenIds, rawEstados);

        assertEquals(5L, result.getTotalDocentes());
        assertNull(result.getMaterias());
        assertNull(result.getUnidadNombre());
        assertNull(result.getTipoUnidadNombre());
        verify(dashboardRepository).countTotalTeachers(null, caracterIds, null, null, origenIds, expectedEstados);
        verify(dashboardRepository).countTeachersByUnitType(null, caracterIds, null, null, origenIds, expectedEstados);
        verify(dashboardRepository).countTeachersByUnit(null, caracterIds, null, null, origenIds, expectedEstados);
        verify(dashboardRepository, never()).findActiveMateriasByUnidad(any());
        verify(dashboardRepository, never()).countTeachersByMateria(any(), any(), any(), any(), any(), any());
        verify(dashboardRepository, never()).findUnidadMetadata(any());
    }

    @Test
    void testGetTeachingMapData_ForAdminUnidad_PopulatesMateriasIncludingZeroAndUnidadMetadata() {
        Integer unidadId = 10;
        List<String> expectedEstados = List.of("activo");

        when(dashboardRepository.countTotalTeachers(unidadId, null, null, null, null, expectedEstados)).thenReturn(3L);
        when(dashboardRepository.countTeachersByUnitType(unidadId, null, null, null, null, expectedEstados))
                .thenReturn(List.<Object[]>of(new Object[]{"Departamento", 3L}));
        when(dashboardRepository.countTeachersByUnit(unidadId, null, null, null, null, expectedEstados))
                .thenReturn(List.<Object[]>of(new Object[]{"Departamento", "Matemática", 3L}));

        List<Object[]> activeMaterias = new ArrayList<>();
        activeMaterias.add(new Object[]{101, "Álgebra"});
        activeMaterias.add(new Object[]{102, "Análisis I"});
        activeMaterias.add(new Object[]{103, "Geometría"});
        when(dashboardRepository.findActiveMateriasByUnidad(unidadId)).thenReturn(activeMaterias);

        List<Object[]> byMateria = new ArrayList<>();
        byMateria.add(new Object[]{101, 2L});
        byMateria.add(new Object[]{103, 1L});
        when(dashboardRepository.countTeachersByMateria(unidadId, null, null, null, null, expectedEstados))
                .thenReturn(byMateria);
        when(dashboardRepository.findUnidadMetadata(unidadId))
                .thenReturn(new Object[]{"Matemática", "Departamento"});

        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(
                unidadId, null, null, null, null, List.of("Activo"));

        assertNotNull(result);
        assertEquals(3L, result.getTotalDocentes());
        assertEquals("Matemática", result.getUnidadNombre());
        assertEquals("Departamento", result.getTipoUnidadNombre());
        assertNotNull(result.getMaterias());
        assertEquals(3, result.getMaterias().size());
        assertEquals("Álgebra", result.getMaterias().get(0).getLabel());
        assertEquals(2L, result.getMaterias().get(0).getValue());
        assertEquals("Análisis I", result.getMaterias().get(1).getLabel());
        assertEquals(0L, result.getMaterias().get(1).getValue());
        assertEquals("Geometría", result.getMaterias().get(2).getLabel());
        assertEquals(1L, result.getMaterias().get(2).getValue());
    }
}

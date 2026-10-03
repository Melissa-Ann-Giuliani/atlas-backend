package com.atlas.atlas_backend.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
        // Arrange
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

        // Act
        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getTotalDocentes());
        assertEquals(3, result.getUnitTypes().size());

        // Assert specific unit mapping
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
        // Arrange
        when(dashboardRepository.countTotalTeachers(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(dashboardRepository.countTeachersByUnitType(any(), any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(dashboardRepository.countTeachersByUnit(any(), any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());

        // Act
        TeachingMapResponseDTO result = dashboardService.getTeachingMapData(null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalDocentes());
        assertEquals(3, result.getUnitTypes().size()); // Should still initialize the core 3 types

        for (UnitTypeStatsDTO type : result.getUnitTypes()) {
            assertEquals(0L, type.getValue());
        }
        assertEquals(0, result.getCentros().size());
        assertEquals(0, result.getDepartamentos().size());
        assertEquals(0, result.getInstitutos().size());
    }
}

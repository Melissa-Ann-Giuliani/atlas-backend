package com.atlas.atlas_backend.designacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignacionController.class)
@ActiveProfiles("test")
@Import(com.atlas.atlas_backend.security.SecurityConfig.class)
@AutoConfigureMockMvc
public class DesignacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DesignacionService designacionService;

    @MockBean
    private com.atlas.atlas_backend.auth.AuthService authService;

    @MockBean
    private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

    @MockBean
    private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

    private DesignacionCreateRequest validRequest;
    private DesignacionResponseDTO expectedDto;

    @BeforeEach
    void setUp() {
        validRequest = new DesignacionCreateRequest();
        validRequest.setDocenteId(10);
        validRequest.setCargoId(25);
        validRequest.setNroResolucion(1024);
        validRequest.setFechaInicio(LocalDate.of(2026, 3, 1));
        validRequest.setFechaFin(LocalDate.of(2027, 2, 28));

        DesignacionResponseDTO.DocenteResumenDTO docenteDto = new DesignacionResponseDTO.DocenteResumenDTO();
        docenteDto.setId(10);
        docenteDto.setNombre("Juan");
        docenteDto.setApellido("Pérez");
        docenteDto.setDni(30123456L);
        docenteDto.setEmail("juan.perez@atlas.edu");

        DesignacionResponseDTO.CargoResumenDTO cargoDto = new DesignacionResponseDTO.CargoResumenDTO();
        cargoDto.setId(25);
        cargoDto.setCodigo(5001L);
        cargoDto.setCategoria("Profesor Titular");
        cargoDto.setDedicacion("Exclusiva");
        cargoDto.setUnidad("Facultad de Ingeniería");

        expectedDto = new DesignacionResponseDTO();
        expectedDto.setId(100);
        expectedDto.setNroResolucion(1024);
        expectedDto.setFechaInicio(LocalDate.of(2026, 3, 1));
        expectedDto.setFechaFin(LocalDate.of(2027, 2, 28));
        expectedDto.setEstado("Activo");
        expectedDto.setDocente(docenteDto);
        expectedDto.setCargo(cargoDto);
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario1_Success_AdminGlobal_Returns201() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenReturn(expectedDto);

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nroResolucion").value(1024))
                .andExpect(jsonPath("$.estado").value("Activo"))
                .andExpect(jsonPath("$.docente.id").value(10))
                .andExpect(jsonPath("$.docente.dni").value(30123456))
                .andExpect(jsonPath("$.cargo.id").value(25))
                .andExpect(jsonPath("$.cargo.codigo").value(5001));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testScenario3_Success_AdminUnidad_Returns201() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_unidad")))
                .thenReturn(expectedDto);

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("Activo"));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testScenario4_Forbidden_AdminUnidad_OtherUnit_Returns403() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_unidad")))
                .thenThrow(new AccessDeniedException(
                        "No tiene permisos para registrar designaciones en cargos de otra unidad"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("No tiene permisos para registrar designaciones en cargos de otra unidad"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario5_DocenteNotFound_Returns404() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Docente no encontrado"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario6_CargoNotFound_Returns404() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cargo no encontrado"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario7_CargoNotLibre_Returns400() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El cargo ya se encuentra asignado o en licencia"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cargo ya se encuentra asignado o en licencia"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario8_DuplicateResolucion_Returns400() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El docente ya posee una designación registrada con el número de resolución indicado"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El docente ya posee una designación registrada con el número de resolución indicado"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testScenario9_InvalidDates_Returns400() throws Exception {
        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de finalización no puede ser anterior a la fecha de inicio"));

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La fecha de finalización no puede ser anterior a la fecha de inicio"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = { "AUDITOR" })
    void testScenario10_AuditorRole_WriteOperationDenied_Returns403() throws Exception {
        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testBeanValidation_MissingCargoId_Returns400() throws Exception {
        validRequest.setCargoId(null);

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "auditor", roles = { "AUDITOR" })
    void testGetDesignacionPorId_AuditorAllowed_Returns200() throws Exception {
        when(designacionService.obtenerPorId(100)).thenReturn(expectedDto);

        mockMvc.perform(get("/api/designaciones/100")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nroResolucion").value(1024));
    }
}


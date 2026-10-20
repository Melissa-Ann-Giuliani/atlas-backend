package com.atlas.atlas_backend.materia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MateriaController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.atlas.atlas_backend.security.SecurityConfig.class)
@AutoConfigureMockMvc
public class MateriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MateriaService materiaService;

    @MockBean
    private com.atlas.atlas_backend.auth.AuthService authService;

    @MockBean
    private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

    @MockBean
    private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

    private MateriaCreateRequest validRequest;
    private MateriaDetailDTO expectedDto;

    @BeforeEach
    void setUp() {
        validRequest = new MateriaCreateRequest();
        validRequest.setNombre("Álgebra Lineal");
        validRequest.setHoras(6);
        validRequest.setAnio("1°");
        validRequest.setDespliegue("Primer Semestre");
        validRequest.setUnidadId(1);
        validRequest.setDependenciaId(10);
        validRequest.setPlanEstudio(2023);

        expectedDto = new MateriaDetailDTO();
        expectedDto.setId(101);
        expectedDto.setNombre("Álgebra Lineal");
        expectedDto.setHoras(6);
        expectedDto.setAnio("1°");
        expectedDto.setDespliegue("Primer Semestre");
        expectedDto.setPlanEstudio(2023);
        expectedDto.setUnidadId(1);
        expectedDto.setUnidadNombre("Matemática");
        expectedDto.setDependenciaId(10);
        expectedDto.setDependenciaNombre("Departamento");
    }

    @Test
    @DisplayName("POST /api/materias - 201 Created for ADMIN_GLOBAL")
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void testCrearMateria_AdminGlobal_Returns201() throws Exception {
        when(materiaService.crearMateria(any(MateriaCreateRequest.class), eq("admin_global")))
                .thenReturn(expectedDto);

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.nombre").value("Álgebra Lineal"))
                .andExpect(jsonPath("$.horas").value(6))
                .andExpect(jsonPath("$.anio").value("1°"))
                .andExpect(jsonPath("$.despliegue").value("Primer Semestre"))
                .andExpect(jsonPath("$.planEstudio").value(2023))
                .andExpect(jsonPath("$.unidadId").value(1))
                .andExpect(jsonPath("$.unidadNombre").value("Matemática"));
    }

    @Test
    @DisplayName("POST /api/materias - 201 Created for ADMIN_UNIDAD")
    @WithMockUser(username = "admin_unidad", roles = {"ADMIN_UNIDAD"})
    void testCrearMateria_AdminUnidad_Returns201() throws Exception {
        when(materiaService.crearMateria(any(MateriaCreateRequest.class), eq("admin_unidad")))
                .thenReturn(expectedDto);

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101));
    }

    @Test
    @DisplayName("POST /api/materias - 403 Forbidden when unit admin acts on unauthorized unit")
    @WithMockUser(username = "admin_unidad", roles = {"ADMIN_UNIDAD"})
    void testCrearMateria_Forbidden_UnitMismatch() throws Exception {
        when(materiaService.crearMateria(any(MateriaCreateRequest.class), eq("admin_unidad")))
                .thenThrow(new AccessDeniedException("No tiene permisos para registrar materias en esta unidad académica"));

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para registrar materias en esta unidad académica"));
    }

    @Test
    @DisplayName("POST /api/materias - 409 Conflict when subject duplicate exists")
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void testCrearMateria_Conflict_Duplicate() throws Exception {
        when(materiaService.crearMateria(any(MateriaCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una materia con ese nombre para esta unidad y plan de estudio"));

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una materia con ese nombre para esta unidad y plan de estudio"));
    }

    @Test
    @DisplayName("POST /api/materias - 404 Not Found when academic unit does not exist")
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void testCrearMateria_NotFound_Unidad() throws Exception {
        when(materiaService.crearMateria(any(MateriaCreateRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad académica no encontrada"));

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unidad académica no encontrada"));
    }

    @Test
    @DisplayName("POST /api/materias - 400 Bad Request on invalid hours (> 40 or < 1)")
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void testCrearMateria_ValidationError_Horas() throws Exception {
        validRequest.setHoras(45);

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/materias - 400 Bad Request on invalid despliegue")
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void testCrearMateria_ValidationError_Despliegue() throws Exception {
        validRequest.setDespliegue("Trimestral");

        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/materias - 403 Forbidden for unauthorized role (e.g. DOCENTE)")
    @WithMockUser(username = "docente", roles = {"DOCENTE"})
    void testCrearMateria_UnauthorizedRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/materias - 403 Forbidden for unauthenticated request")
    void testCrearMateria_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/materias")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isForbidden());
    }
}

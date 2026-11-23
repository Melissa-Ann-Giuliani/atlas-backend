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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockBean
    private com.atlas.atlas_backend.usuario.UsuarioRepository usuarioRepository;

    private DesignacionCreateRequest validRequest;
    private DesignacionResponseDTO expectedDto;

    @BeforeEach
    void setUp() {
        validRequest = new DesignacionCreateRequest();
        validRequest.setDocenteId(10);
        validRequest.setCargoId(25);
        validRequest.setNroResolucion("1024");
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
        expectedDto.setNroResolucion("1024");
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
                .andExpect(jsonPath("$.nroResolucion").value("1024"))
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
    void testWithoutCargoId_Success_Returns201() throws Exception {
        validRequest.setCargoId(null);
        expectedDto.setCargo(null);

        when(designacionService.registrarDesignacion(any(DesignacionCreateRequest.class), eq("admin_global")))
                .thenReturn(expectedDto);

        mockMvc.perform(post("/api/designaciones")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nroResolucion").value("1024"))
                .andExpect(jsonPath("$.cargo").doesNotExist());
    }

    @Test
    @WithMockUser(username = "auditor", roles = { "AUDITOR" })
    void testGetDesignacionPorId_AuditorAllowed_Returns200() throws Exception {
        when(designacionService.obtenerPorId(100)).thenReturn(expectedDto);

        mockMvc.perform(get("/api/designaciones/100")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nroResolucion").value("1024"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_Success_AdminGlobal_Returns200() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(25);
        when(designacionService.vincularCargo(100, 25, "admin_global")).thenReturn(expectedDto);

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.cargo.id").value(25))
                .andExpect(jsonPath("$.cargo.codigo").value(5001));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testVincularCargo_Success_AdminUnidad_Returns200() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(25);
        when(designacionService.vincularCargo(100, 25, "admin_unidad")).thenReturn(expectedDto);

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testVincularCargo_Forbidden_AdminUnidad_OtherUnit_Returns403() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(30);
        when(designacionService.vincularCargo(100, 30, "admin_unidad"))
                .thenThrow(new AccessDeniedException("No tiene permisos para vincular cargos de otra unidad"));

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para vincular cargos de otra unidad"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_CargoNotLibre_Returns400() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(25);
        when(designacionService.vincularCargo(100, 25, "admin_global"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cargo ya se encuentra asignado o en licencia"));

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cargo ya se encuentra asignado o en licencia"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_CargoNotFound_Returns404() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(999);
        when(designacionService.vincularCargo(100, 999, "admin_global"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cargo no encontrado"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_DesignacionNotFound_Returns404() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(25);
        when(designacionService.vincularCargo(999, 25, "admin_global"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Designación no encontrada"));

        mockMvc.perform(patch("/api/designaciones/999/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Designación no encontrada"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_DesignacionAlreadyHasCargo_Returns400() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(26);
        when(designacionService.vincularCargo(100, 26, "admin_global"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La designación ya posee un cargo vinculado"));

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La designación ya posee un cargo vinculado"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testVincularCargo_NullCargoId_Returns400() throws Exception {
        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "auditor", roles = { "AUDITOR" })
    void testVincularCargo_AuditorRole_Forbidden_Returns403() throws Exception {
        DesignacionVincularCargoRequest req = new DesignacionVincularCargoRequest(25);

        mockMvc.perform(patch("/api/designaciones/100/cargo")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // RF-36: Endpoints /vencimientos y /vencimientos/resumen
    // =========================================================================

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testRF36_GetVencimientos_AdminGlobal_Returns200() throws Exception {
        DesignacionVencimientoDTO item = DesignacionVencimientoDTO.builder()
                .designacionId(142)
                .numeroResolucion("RES-2024-510")
                .fechaInicio(LocalDate.of(2024, 3, 1))
                .fechaFin(LocalDate.of(2026, 11, 30))
                .diasRestantes(14L)
                .estadoActual("Activa")
                .docente(DesignacionVencimientoDTO.DocenteResumenDTO.builder()
                        .docenteId(45)
                        .nombre("Juan")
                        .apellido("Pérez")
                        .documento("28123456")
                        .legajo("9821")
                        .build())
                .unidad(DesignacionVencimientoDTO.UnidadResumenDTO.builder()
                        .unidadId(2)
                        .nombre("Facultad de Ingeniería")
                        .build())
                .cargos(java.util.List.of(
                        DesignacionVencimientoDTO.CargoResumenDTO.builder()
                                .cargoId(88)
                                .codigo(100234L)
                                .categoria("Profesor Adjunto")
                                .dedicacion("Simple (10 hs)")
                                .caracter("Interino")
                                .build()))
                .build();

        when(designacionService.obtenerVencimientos(null, false, null, "admin_global"))
                .thenReturn(java.util.List.of(item));

        mockMvc.perform(get("/api/designaciones/vencimientos")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].designacionId").value(142))
                .andExpect(jsonPath("$[0].numeroResolucion").value("RES-2024-510"))
                .andExpect(jsonPath("$[0].diasRestantes").value(14))
                .andExpect(jsonPath("$[0].estadoActual").value("Activa"))
                .andExpect(jsonPath("$[0].docente.docenteId").value(45))
                .andExpect(jsonPath("$[0].docente.documento").value("28123456"))
                .andExpect(jsonPath("$[0].unidad.unidadId").value(2))
                .andExpect(jsonPath("$[0].cargos[0].cargoId").value(88))
                .andExpect(jsonPath("$[0].cargos[0].codigo").value(100234));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testRF36_GetVencimientos_AdminUnidad_OtherUnit_Returns403() throws Exception {
        when(designacionService.obtenerVencimientos(3, false, null, "admin_unidad"))
                .thenThrow(new AccessDeniedException("No tiene permisos para consultar designaciones de otra unidad"));

        mockMvc.perform(get("/api/designaciones/vencimientos?unidadId=3")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para consultar designaciones de otra unidad"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testRF36_GetResumenVencimientos_AdminGlobal_Returns200() throws Exception {
        DesignacionVencimientosResumenDTO resumen = DesignacionVencimientosResumenDTO.builder()
                .totalPorVencer(7L)
                .umbralDias(30)
                .unidadId(null)
                .unidadNombre(null)
                .tieneVencimientosCriticos(true)
                .totalCriticos(2L)
                .build();

        when(designacionService.obtenerResumenVencimientos(null, "admin_global"))
                .thenReturn(resumen);

        mockMvc.perform(get("/api/designaciones/vencimientos/resumen")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPorVencer").value(7))
                .andExpect(jsonPath("$.umbralDias").value(30))
                .andExpect(jsonPath("$.tieneVencimientosCriticos").value(true))
                .andExpect(jsonPath("$.totalCriticos").value(2));
    }

    // =========================================================================
    // RF-37: Endpoint POST /api/designaciones/{id}/redesignar
    // =========================================================================

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testRF37_Redesignar_Success_AdminUnidad_Returns201() throws Exception {
        DesignacionRedesignarRequest req = new DesignacionRedesignarRequest();
        req.setNroResolucion("RES-2026-890");
        req.setFechaInicio(LocalDate.of(2026, 12, 1));
        req.setFechaFin(LocalDate.of(2027, 11, 30));
        req.setCargoId(12);

        DesignacionResponseDTO resDto = new DesignacionResponseDTO();
        resDto.setId(185);
        resDto.setDesignacionId(185);
        resDto.setNroResolucion("RES-2026-890");
        resDto.setFechaInicio(LocalDate.of(2026, 12, 1));
        resDto.setFechaFin(LocalDate.of(2027, 11, 30));
        resDto.setEstado("Activa");
        resDto.setDocenteId(45);
        resDto.setCargoId(12);

        when(designacionService.redesignarDocente(eq(10), any(DesignacionRedesignarRequest.class), eq("admin_unidad")))
                .thenReturn(resDto);

        mockMvc.perform(post("/api/designaciones/10/redesignar")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designacionId").value(185))
                .andExpect(jsonPath("$.nroResolucion").value("RES-2026-890"))
                .andExpect(jsonPath("$.estado").value("Activa"))
                .andExpect(jsonPath("$.docenteId").value(45))
                .andExpect(jsonPath("$.cargoId").value(12));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
    void testRF37_Redesignar_AdminUnidad_OtherUnit_Returns403() throws Exception {
        DesignacionRedesignarRequest req = new DesignacionRedesignarRequest();
        req.setNroResolucion("RES-2026-890");
        req.setFechaInicio(LocalDate.of(2026, 12, 1));

        when(designacionService.redesignarDocente(eq(20), any(DesignacionRedesignarRequest.class), eq("admin_unidad")))
                .thenThrow(new AccessDeniedException("No tiene permisos para re-designar cargos de otra unidad"));

        mockMvc.perform(post("/api/designaciones/20/redesignar")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para re-designar cargos de otra unidad"));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
    void testRF37_Redesignar_DuplicateResolucion_Returns400() throws Exception {
        DesignacionRedesignarRequest req = new DesignacionRedesignarRequest();
        req.setNroResolucion("RES-2024-500");
        req.setFechaInicio(LocalDate.of(2026, 12, 1));

        when(designacionService.redesignarDocente(eq(10), any(DesignacionRedesignarRequest.class), eq("admin_global")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El docente ya posee una designación registrada con el número de resolución indicado"));

        mockMvc.perform(post("/api/designaciones/10/redesignar")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El docente ya posee una designación registrada con el número de resolución indicado"));
    }
}


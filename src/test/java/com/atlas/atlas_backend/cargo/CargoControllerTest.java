package com.atlas.atlas_backend.cargo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CargoController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.atlas.atlas_backend.security.SecurityConfig.class)
@AutoConfigureMockMvc
public class CargoControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CargoService cargoService;

        @MockBean
        private com.atlas.atlas_backend.auth.AuthService authService;

        @MockBean
        private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

        @MockBean
        private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

        private CargoCreateRequest validRequest;
        private CargoUpdateRequest validUpdateRequest;
        private CargoDetailDTO expectedDto;

        @BeforeEach
        void setUp() {
                validRequest = new CargoCreateRequest();
                validRequest.setCodigo(5001L);
                validRequest.setFechaCreacion(LocalDate.of(2026, 10, 18));
                validRequest.setOrigenId(1);
                validRequest.setCaracterId(1);
                validRequest.setCategoriaId(1);
                validRequest.setDedicacionId(1);
                validRequest.setUnidadId(1);

                validUpdateRequest = new CargoUpdateRequest();
                validUpdateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
                validUpdateRequest.setOrigenId(1);
                validUpdateRequest.setCaracterId(1);
                validUpdateRequest.setCategoriaId(1);
                validUpdateRequest.setDedicacionId(1);

                expectedDto = new CargoDetailDTO();
                expectedDto.setNumero(5001L);
                expectedDto.setEstado("Libre");
                expectedDto.setCategoria("Profesor Titular");
                expectedDto.setDedicacion("Exclusiva");
                expectedDto.setCaracter("Ordinario");
                expectedDto.setFechaCreacion(LocalDate.of(2026, 10, 18));
                expectedDto.setOrigenId(1);
                expectedDto.setOrigen("Tesoro");
                expectedDto.setUnidadId(1);
                expectedDto.setUnidadNombre("Facultad de Ingeniería");
                expectedDto.setTipoUnidadNombre("Facultad");
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario1_Success_StandAloneCargo_Returns201() throws Exception {
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_global")))
                                .thenReturn(expectedDto);

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.numero").value(5001))
                                .andExpect(jsonPath("$.estado").value("Libre"))
                                .andExpect(jsonPath("$.categoria").value("Profesor Titular"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario2_Success_WithPredecessor_Returns201() throws Exception {
                validRequest.setPredecesorCodigo(4001L);

                CargoDetailDTO.CargoPredecesorData predecesorData = new CargoDetailDTO.CargoPredecesorData();
                predecesorData.setNumero(4001L);
                predecesorData.setEstado("Baja");
                expectedDto.setCargoPredecesor(predecesorData);

                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_global")))
                                .thenReturn(expectedDto);

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.numero").value(5001))
                                .andExpect(jsonPath("$.cargoPredecesor.numero").value(4001));
        }

        @Test
        @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
        void testScenario3_Success_AdminUnidad_OwnUnit_Returns201() throws Exception {
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_unidad")))
                                .thenReturn(expectedDto);

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.numero").value(5001))
                                .andExpect(jsonPath("$.estado").value("Libre"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario4_Conflict_DuplicateCodigo_Returns409() throws Exception {
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                                                "El número de cargo ya existe"));

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El número de cargo ya existe"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario5_Validation_MissingRequiredField_Returns400() throws Exception {
                validRequest.setCodigo(null);

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario6_Error_InvalidPredecessor_Returns404() throws Exception {
                validRequest.setPredecesorCodigo(9999L);
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Cargo predecesor no encontrado"));

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Cargo predecesor no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testScenario7_Error_NonExistentForeignKey_Returns404() throws Exception {
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Origen no encontrado"));

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Origen no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
        void testScenario8_Forbidden_AdminUnidad_CrossUnit_Returns403() throws Exception {
                when(cargoService.crearCargo(any(CargoCreateRequest.class), eq("admin_unidad")))
                                .thenThrow(new AccessDeniedException(
                                                "No tiene permisos para registrar cargos en esta unidad"));

                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message")
                                                .value("No tiene permisos para registrar cargos en esta unidad"));
        }

        @Test
        @WithMockUser(username = "docente", roles = { "DOCENTE" })
        void testScenario9_UnauthorizedRole_Returns403() throws Exception {
                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testScenario10_Unauthenticated_ReturnsForbidden() throws Exception {
                mockMvc.perform(post("/api/cargos")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testPutModificarCargo_Success_Returns200() throws Exception {
                when(cargoService.modificarCargo(eq(10), any(CargoUpdateRequest.class), eq("admin_global")))
                                .thenReturn(expectedDto);

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.numero").value(5001))
                                .andExpect(jsonPath("$.estado").value("Libre"))
                                .andExpect(jsonPath("$.categoria").value("Profesor Titular"));
        }

        @Test
        @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
        void testPutModificarCargo_AdminUnidad_OwnUnit_Returns200() throws Exception {
                when(cargoService.modificarCargo(eq(10), any(CargoUpdateRequest.class), eq("admin_unidad")))
                                .thenReturn(expectedDto);

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.numero").value(5001));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testPutModificarCargo_Validation_MissingRequiredField_Returns400() throws Exception {
                validUpdateRequest.setCategoriaId(null);

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testPutModificarCargo_DedicacionHoursConflict_Returns400() throws Exception {
                when(cargoService.modificarCargo(eq(10), any(CargoUpdateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Las horas de la dedicación no pueden ser inferiores a las horas ocupadas"));

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Las horas de la dedicación no pueden ser inferiores a las horas ocupadas"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testPutModificarCargo_NonExistentCargo_Returns404() throws Exception {
                when(cargoService.modificarCargo(eq(9999), any(CargoUpdateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

                mockMvc.perform(put("/api/cargos/9999")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Cargo no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testPutModificarCargo_NonExistentCatalogue_Returns404() throws Exception {
                when(cargoService.modificarCargo(eq(10), any(CargoUpdateRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Origen no encontrado"));

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Origen no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
        void testPutModificarCargo_Forbidden_AdminUnidad_CrossUnit_Returns403() throws Exception {
                when(cargoService.modificarCargo(eq(10), any(CargoUpdateRequest.class), eq("admin_unidad")))
                                .thenThrow(new AccessDeniedException(
                                                "No tiene permisos para modificar cargos en esta unidad"));

                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message")
                                                .value("No tiene permisos para modificar cargos en esta unidad"));
        }

        @Test
        @WithMockUser(username = "docente", roles = { "DOCENTE" })
        void testPutModificarCargo_UnauthorizedRole_Returns403() throws Exception {
                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testPutModificarCargo_Unauthenticated_ReturnsForbidden() throws Exception {
                mockMvc.perform(put("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testGetCargoDetails_Success_Returns200WithOriginAndUnit() throws Exception {
                when(cargoService.getCargoDetails(eq(10), eq("admin_global"))).thenReturn(expectedDto);

                mockMvc.perform(get("/api/cargos/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.numero").value(5001))
                                .andExpect(jsonPath("$.origenId").value(1))
                                .andExpect(jsonPath("$.origen").value("Tesoro"))
                                .andExpect(jsonPath("$.unidadId").value(1))
                                .andExpect(jsonPath("$.unidadNombre").value("Facultad de Ingeniería"))
                                .andExpect(jsonPath("$.tipoUnidadNombre").value("Facultad"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testGetCargoDetails_NotFound_Returns404() throws Exception {
                when(cargoService.getCargoDetails(eq(9999), eq("admin_global")))
                                .thenThrow(new Exception("Cargo no encontrado"));

                mockMvc.perform(get("/api/cargos/9999")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                                .andExpect(status().isNotFound());
        }

        // =========================================================================
        // POST /api/cargos/subdividir TESTS (RF-19)
        // =========================================================================

        private SubdividirCargoRequest buildValidSubdividirRequest() {
                SubdividirCargoRequest req = new SubdividirCargoRequest();
                req.setCargoOrigenCodigo(8001L);

                SubcargoItemRequest item1 = new SubcargoItemRequest();
                item1.setCodigo(8002L);
                item1.setFechaCreacion(LocalDate.of(2026, 10, 19));
                item1.setOrigenId(1);
                item1.setCaracterId(1);
                item1.setCategoriaId(1);
                item1.setDedicacionId(1);
                item1.setUnidadId(1);

                SubcargoItemRequest item2 = new SubcargoItemRequest();
                item2.setCodigo(8003L);
                item2.setFechaCreacion(LocalDate.of(2026, 10, 19));
                item2.setOrigenId(1);
                item2.setCaracterId(1);
                item2.setCategoriaId(1);
                item2.setDedicacionId(1);
                item2.setUnidadId(1);

                req.setSubcargos(java.util.List.of(item1, item2));
                return req;
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testSubdividirCargo_Success_Returns201() throws Exception {
                SubdividirCargoRequest req = buildValidSubdividirRequest();

                CargoDetailDTO sc1 = new CargoDetailDTO();
                sc1.setNumero(8002L);
                sc1.setEstado("Libre");

                CargoDetailDTO sc2 = new CargoDetailDTO();
                sc2.setNumero(8003L);
                sc2.setEstado("Libre");

                when(cargoService.subdividirCargo(any(SubdividirCargoRequest.class), eq("admin_global")))
                                .thenReturn(java.util.List.of(sc1, sc2));

                mockMvc.perform(post("/api/cargos/subdividir")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].numero").value(8002))
                                .andExpect(jsonPath("$[1].numero").value(8003));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testSubdividirCargo_BadRequest_Returns400() throws Exception {
                SubdividirCargoRequest req = buildValidSubdividirRequest();

                when(cargoService.subdividirCargo(any(SubdividirCargoRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suma de horas supera al original"));

                mockMvc.perform(post("/api/cargos/subdividir")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La suma de horas supera al original"));
        }

        @Test
        @WithMockUser(username = "admin_global", roles = { "ADMIN_GLOBAL" })
        void testSubdividirCargo_Conflict_Returns409() throws Exception {
                SubdividirCargoRequest req = buildValidSubdividirRequest();

                when(cargoService.subdividirCargo(any(SubdividirCargoRequest.class), eq("admin_global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El código 8002 ya existe"));

                mockMvc.perform(post("/api/cargos/subdividir")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El código 8002 ya existe"));
        }

        @Test
        @WithMockUser(username = "admin_unidad", roles = { "ADMIN_UNIDAD" })
        void testSubdividirCargo_AccessDenied_Returns403() throws Exception {
                SubdividirCargoRequest req = buildValidSubdividirRequest();

                when(cargoService.subdividirCargo(any(SubdividirCargoRequest.class), eq("admin_unidad")))
                                .thenThrow(new AccessDeniedException("No tiene permisos"));

                mockMvc.perform(post("/api/cargos/subdividir")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("No tiene permisos"));
        }

        @Test
        @WithMockUser(username = "docente", roles = { "DOCENTE" })
        void testSubdividirCargo_UnauthorizedRole_Returns403() throws Exception {
                SubdividirCargoRequest req = buildValidSubdividirRequest();

                mockMvc.perform(post("/api/cargos/subdividir")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                                .andExpect(status().isForbidden());
        }
}


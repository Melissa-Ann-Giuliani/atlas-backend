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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

                expectedDto = new CargoDetailDTO();
                expectedDto.setNumero(5001L);
                expectedDto.setEstado("Libre");
                expectedDto.setCategoria("Profesor Titular");
                expectedDto.setDedicacion("Exclusiva");
                expectedDto.setCaracter("Ordinario");
                expectedDto.setFechaCreacion(LocalDate.of(2026, 10, 18));
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
}

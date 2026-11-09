package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.catedra.dto.CatedraRequestDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraResponseDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraUpdateRequestDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatedraController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.atlas.atlas_backend.security.SecurityConfig.class)
@AutoConfigureMockMvc
public class CatedraControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CatedraService catedraService;

        @MockBean
        private com.atlas.atlas_backend.auth.AuthService authService;

        @MockBean
        private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

        @MockBean
        private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

        private CatedraRequestDTO validRequest;
        private CatedraUpdateRequestDTO validUpdateRequest;
        private CatedraResponseDTO expectedResponse;
        private CatedraResponseDTO expectedUpdatedResponse;

        @BeforeEach
        void setUp() {
                validRequest = CatedraRequestDTO.builder()
                                .nombre("Cátedra A")
                                .tipologia(1)
                                .directorId(10)
                                .materiaId(1)
                                .build();

                validUpdateRequest = CatedraUpdateRequestDTO.builder()
                                .nombre("Cátedra A Renovada")
                                .tipologia(2)
                                .directorId(12)
                                .build();

                expectedResponse = CatedraResponseDTO.builder()
                                .id(100)
                                .nombre("Cátedra A")
                                .tipologia(1)
                                .estado("Activa")
                                .directorId(10)
                                .directorNombre("Carlos Pérez")
                                .materiaId(1)
                                .materiaNombre("Programación I")
                                .build();

                expectedUpdatedResponse = CatedraResponseDTO.builder()
                                .id(10)
                                .nombre("Cátedra A Renovada")
                                .tipologia(2)
                                .estado("Activa")
                                .directorId(12)
                                .directorNombre("Laura Martínez")
                                .materiaId(1)
                                .materiaNombre("Programación I")
                                .build();
        }

        @Test
        @DisplayName("POST /api/catedras - 201 Created con rol ADMIN_GLOBAL")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void crearCatedra_AdminGlobal_Retorna201() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.global")))
                                .thenReturn(expectedResponse);

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(100))
                                .andExpect(jsonPath("$.nombre").value("Cátedra A"))
                                .andExpect(jsonPath("$.tipologia").value(1))
                                .andExpect(jsonPath("$.estado").value("Activa"))
                                .andExpect(jsonPath("$.directorId").value(10))
                                .andExpect(jsonPath("$.directorNombre").value("Carlos Pérez"))
                                .andExpect(jsonPath("$.materiaId").value(1))
                                .andExpect(jsonPath("$.materiaNombre").value("Programación I"));
        }

        @Test
        @DisplayName("POST /api/catedras - 201 Created con rol ADMIN_UNIDAD")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void crearCatedra_AdminUnidad_Retorna201() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.unidad")))
                                .thenReturn(expectedResponse);

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(100))
                                .andExpect(jsonPath("$.estado").value("Activa"));
        }

        @Test
        @DisplayName("POST /api/catedras - 403 Forbidden cuando el usuario no tiene rol autorizado")
        @WithMockUser(username = "docente.user", roles = { "DOCENTE" })
        void crearCatedra_RolNoAutorizado_Retorna403() throws Exception {
                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/catedras - 403 Forbidden cuando Admin de Unidad no tiene permisos sobre la unidad")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void crearCatedra_AdminUnidadMateriaOtraUnidad_Retorna403() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.unidad")))
                                .thenThrow(new AccessDeniedException(
                                                "No tiene permisos para crear cátedras en materias de otra unidad"));

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value(
                                                "No tiene permisos para crear cátedras en materias de otra unidad"));
        }

        @Test
        @DisplayName("POST /api/catedras - 400 Bad Request por nombre duplicado")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void crearCatedra_NombreDuplicado_Retorna400() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Ya existe una cátedra con ese nombre para la materia seleccionada"));

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Ya existe una cátedra con ese nombre para la materia seleccionada"));
        }

        @Test
        @DisplayName("POST /api/catedras - 400 Bad Request por docente inactivo")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void crearCatedra_DocenteInactivo_Retorna400() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "El docente debe estar en estado activo"));

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("El docente debe estar en estado activo"));
        }

        @Test
        @DisplayName("POST /api/catedras - 404 Not Found cuando materia o docente no existen")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void crearCatedra_MateriaNoEncontrada_Retorna404() throws Exception {
                when(catedraService.crearCatedra(any(CatedraRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Materia no encontrada"));
        }

        @Test
        @DisplayName("POST /api/catedras - 400 Bad Request por validación de campos (@NotBlank, @Min, @Max)")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void crearCatedra_CamposInvalidos_Retorna400() throws Exception {
                CatedraRequestDTO invalid = CatedraRequestDTO.builder()
                                .nombre("")
                                .tipologia(5) // Max es 4
                                .directorId(null)
                                .materiaId(null)
                                .build();

                mockMvc.perform(post("/api/catedras")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalid))))
                                .andExpect(status().isBadRequest());
        }

        // =========================================================================
        // RF-22: PUT /api/catedras/{id} - Modificar los datos de una cátedra
        // =========================================================================

        @Test
        @DisplayName("PUT /api/catedras/{id} - 200 OK con rol ADMIN_GLOBAL")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_AdminGlobal_Retorna200() throws Exception {
                when(catedraService.modificarCatedra(eq(10), any(CatedraUpdateRequestDTO.class), eq("admin.global")))
                                .thenReturn(expectedUpdatedResponse);

                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.nombre").value("Cátedra A Renovada"))
                                .andExpect(jsonPath("$.tipologia").value(2))
                                .andExpect(jsonPath("$.estado").value("Activa"))
                                .andExpect(jsonPath("$.directorId").value(12))
                                .andExpect(jsonPath("$.directorNombre").value("Laura Martínez"))
                                .andExpect(jsonPath("$.materiaId").value(1))
                                .andExpect(jsonPath("$.materiaNombre").value("Programación I"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 200 OK con rol ADMIN_UNIDAD")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void modificarCatedra_AdminUnidad_Retorna200() throws Exception {
                when(catedraService.modificarCatedra(eq(10), any(CatedraUpdateRequestDTO.class), eq("admin.unidad")))
                                .thenReturn(expectedUpdatedResponse);

                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.nombre").value("Cátedra A Renovada"))
                                .andExpect(jsonPath("$.estado").value("Activa"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 403 Forbidden cuando el usuario no tiene rol autorizado")
        @WithMockUser(username = "docente.user", roles = { "DOCENTE" })
        void modificarCatedra_RolNoAutorizado_Retorna403() throws Exception {
                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 403 Forbidden cuando Admin de Unidad intenta modificar cátedra de otra unidad")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void modificarCatedra_AdminUnidadOtraUnidad_Retorna403() throws Exception {
                when(catedraService.modificarCatedra(eq(30), any(CatedraUpdateRequestDTO.class), eq("admin.unidad")))
                                .thenThrow(new AccessDeniedException(
                                                "No tiene permisos para modificar cátedras en materias de otra unidad"));

                mockMvc.perform(put("/api/catedras/30")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value(
                                                "No tiene permisos para modificar cátedras en materias de otra unidad"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 404 Not Found cuando la cátedra no existe")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_CatedraNoEncontrada_Retorna404() throws Exception {
                when(catedraService.modificarCatedra(eq(9999), any(CatedraUpdateRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cátedra no encontrada"));

                mockMvc.perform(put("/api/catedras/9999")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Cátedra no encontrada"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 400 Bad Request cuando la cátedra está de baja")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_CatedraDeBaja_Retorna400() throws Exception {
                when(catedraService.modificarCatedra(eq(10), any(CatedraUpdateRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "No se puede modificar una cátedra que no se encuentra en estado activa"));

                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "No se puede modificar una cátedra que no se encuentra en estado activa"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 400 Bad Request por nombre duplicado en la materia")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_NombreDuplicado_Retorna400() throws Exception {
                when(catedraService.modificarCatedra(eq(11), any(CatedraUpdateRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Ya existe una cátedra con ese nombre para la materia seleccionada"));

                mockMvc.perform(put("/api/catedras/11")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Ya existe una cátedra con ese nombre para la materia seleccionada"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 400 Bad Request por docente inactivo")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_DocenteInactivo_Retorna400() throws Exception {
                when(catedraService.modificarCatedra(eq(10), any(CatedraUpdateRequestDTO.class), eq("admin.global")))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "El docente debe estar en estado activo"));

                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(validUpdateRequest))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("El docente debe estar en estado activo"));
        }

        @Test
        @DisplayName("PUT /api/catedras/{id} - 400 Bad Request por validación de campos (@NotBlank, @Min, @Max, @NotNull)")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void modificarCatedra_CamposInvalidos_Retorna400() throws Exception {
                CatedraUpdateRequestDTO invalid = CatedraUpdateRequestDTO.builder()
                                .nombre("")
                                .tipologia(0) // Min es 1
                                .directorId(null)
                                .build();

                mockMvc.perform(put("/api/catedras/10")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalid))))
                                .andExpect(status().isBadRequest());
        }

        // ===================== RF-23: Baja de cátedra =====================

        private CatedraResponseDTO bajaResponse() {
                return CatedraResponseDTO.builder()
                                .id(10)
                                .nombre("Cátedra A")
                                .tipologia(1)
                                .estado("De baja")
                                .directorId(10)
                                .directorNombre("Carlos Pérez")
                                .materiaId(1)
                                .materiaNombre("Programación I")
                                .build();
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 200 OK con rol ADMIN_GLOBAL")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void darDeBajaCatedra_AdminGlobal_Retorna200() throws Exception {
                when(catedraService.darDeBajaCatedra(10, "admin.global")).thenReturn(bajaResponse());

                mockMvc.perform(patch("/api/catedras/10/baja"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.estado").value("De baja"))
                                .andExpect(jsonPath("$.nombre").value("Cátedra A"))
                                .andExpect(jsonPath("$.tipologia").value(1))
                                .andExpect(jsonPath("$.directorId").value(10))
                                .andExpect(jsonPath("$.materiaId").value(1));
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 200 OK con rol ADMIN_UNIDAD")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void darDeBajaCatedra_AdminUnidad_Retorna200() throws Exception {
                when(catedraService.darDeBajaCatedra(10, "admin.unidad")).thenReturn(bajaResponse());

                mockMvc.perform(patch("/api/catedras/10/baja"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("De baja"));
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 403 Forbidden cuando Admin de Unidad opera sobre otra unidad")
        @WithMockUser(username = "admin.unidad", roles = { "ADMIN_UNIDAD" })
        void darDeBajaCatedra_AdminUnidadOtraUnidad_Retorna403() throws Exception {
                when(catedraService.darDeBajaCatedra(30, "admin.unidad"))
                                .thenThrow(new AccessDeniedException(
                                                "No tiene permisos para dar de baja cátedras en materias de otra unidad"));

                mockMvc.perform(patch("/api/catedras/30/baja"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value(
                                                "No tiene permisos para dar de baja cátedras en materias de otra unidad"));
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 404 Not Found cuando la cátedra no existe")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void darDeBajaCatedra_NoExiste_Retorna404() throws Exception {
                when(catedraService.darDeBajaCatedra(999, "admin.global"))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cátedra no encontrada"));

                mockMvc.perform(patch("/api/catedras/999/baja"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Cátedra no encontrada"));
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 400 Bad Request cuando la cátedra ya está dada de baja")
        @WithMockUser(username = "admin.global", roles = { "ADMIN_GLOBAL" })
        void darDeBajaCatedra_YaDeBaja_Retorna400() throws Exception {
                when(catedraService.darDeBajaCatedra(11, "admin.global"))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "La cátedra ya se encuentra dada de baja"));

                mockMvc.perform(patch("/api/catedras/11/baja"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La cátedra ya se encuentra dada de baja"));
        }

        @Test
        @DisplayName("PATCH /api/catedras/{id}/baja - 403 Forbidden cuando el usuario no tiene rol autorizado")
        @WithMockUser(username = "docente.user", roles = { "DOCENTE" })
        void darDeBajaCatedra_RolNoAutorizado_Retorna403() throws Exception {
                mockMvc.perform(patch("/api/catedras/10/baja"))
                                .andExpect(status().isForbidden());

                verify(catedraService, never()).darDeBajaCatedra(any(), any());
        }
}

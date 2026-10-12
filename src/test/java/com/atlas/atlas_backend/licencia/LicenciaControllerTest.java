package com.atlas.atlas_backend.licencia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LicenciaController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.atlas.atlas_backend.security.SecurityConfig.class)
@AutoConfigureMockMvc
public class LicenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LicenciaService licenciaService;

    @MockBean
    private com.atlas.atlas_backend.auth.AuthService authService;

    @MockBean
    private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

    @MockBean
    private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN_GLOBAL"})
    void getActiveLicencias_AuthorizedRole_Returns200() throws Exception {
        LicenciaListDTO dto = new LicenciaListDTO() {
            public Integer getId() { return 1; }
            public String getNroResolucion() { return "RES-2026-100"; }
            public String getMotivoNombre() { return "Maternidad"; }
            public String getDocenteNombreCompleto() { return "Perez, Juan"; }
            public LocalDate getFechaInicio() { return LocalDate.of(2026, 5, 1); }
            public LocalDate getFechaFin() { return LocalDate.of(2026, 7, 31); }
            public Integer getDiasUsados() { return 90; }
            public String getEstado() { return "Activa"; }
            public Integer getDocenteId() { return 5; }
        };

        Page<LicenciaListDTO> page = new PageImpl<>(List.of(dto));
        when(licenciaService.getActiveLicencias(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/licencias")
                        .param("search", "Perez")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nroResolucion").value("RES-2026-100"))
                .andExpect(jsonPath("$.content[0].motivoNombre").value("Maternidad"))
                .andExpect(jsonPath("$.content[0].docenteNombreCompleto").value("Perez, Juan"))
                .andExpect(jsonPath("$.content[0].estado").value("Activa"));
    }

    @Test
    @WithMockUser(username = "docente", roles = {"DOCENTE"})
    void getActiveLicencias_UnauthorizedRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/licencias"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveLicencias_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/licencias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN_GLOBAL"})
    void getLicenciaDetails_Success_Returns200() throws Exception {
        LicenciaDetailDTO dto = new LicenciaDetailDTO();
        dto.setId(1);
        dto.setNroResolucion("RES-2026-100");
        dto.setMotivoNombre("Maternidad");
        dto.setDocenteNombreCompleto("Gutierrez, Pablo");
        dto.setDocenteDni(40111222L);
        dto.setEstado("Activa");

        when(licenciaService.getLicenciaDetails(1, "admin")).thenReturn(dto);

        mockMvc.perform(get("/api/licencias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nroResolucion").value("RES-2026-100"))
                .andExpect(jsonPath("$.docenteNombreCompleto").value("Gutierrez, Pablo"))
                .andExpect(jsonPath("$.docenteDni").value(40111222));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN_GLOBAL"})
    void getLicenciaDetails_NotFound_Returns404() throws Exception {
        when(licenciaService.getLicenciaDetails(99, "admin"))
                .thenThrow(new NoSuchElementException("No existe"));

        mockMvc.perform(get("/api/licencias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = {"ADMIN_UNIDAD"})
    void getLicenciaDetails_ForbiddenUnit_Returns403() throws Exception {
        when(licenciaService.getLicenciaDetails(1, "admin_unidad"))
                .thenThrow(new AccessDeniedException("No pertenece a su unidad"));

        mockMvc.perform(get("/api/licencias/1"))
                .andExpect(status().isForbidden());
    }
}

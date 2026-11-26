package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.export.ExportFormat;
import com.atlas.atlas_backend.export.ExportService;
import com.atlas.atlas_backend.security.CustomUserDetailsService;
import com.atlas.atlas_backend.security.JwtUtil;
import com.atlas.atlas_backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CambioController.class)
@Import({SecurityConfig.class, ExportService.class})
@SuppressWarnings("null")
class CambioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CambioService cambioService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.atlas.atlas_backend.usuario.UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void consultarHistorial_AdminGlobal_Retorna200() throws Exception {
        CambioResponseDTO dto = CambioResponseDTO.builder()
                .id(1)
                .fechaHora(LocalDateTime.of(2026, 5, 1, 14, 0))
                .categoria("Docente")
                .campoAfectado("domicilio")
                .valorAnterior("Calle A")
                .valorNuevo("Calle B")
                .tipoOperacion("MODIFICACION")
                .usuarioAfectadoId(10)
                .usuarioAfectadoNombreCompleto("Perez, Juan")
                .unidadId(1)
                .build();

        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/cambios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].categoria").value("Docente"))
                .andExpect(jsonPath("$.content[0].tipoOperacion").value("MODIFICACION"));
    }

    @Test
    @WithMockUser(username = "admin_unidad", roles = {"ADMIN_UNIDAD"})
    void consultarHistorial_AdminUnidad_Retorna200() throws Exception {
        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/cambios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "auditor1", roles = {"AUDITOR"})
    void consultarHistorial_AuditorConFiltros_Retorna200() throws Exception {
        when(cambioService.consultarHistorial(
                eq("Perez"),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 1, 31)),
                eq(TipoOperacion.MODIFICACION),
                eq(5),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/cambios")
                        .param("usuarioAfectado", "Perez")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-01-31")
                        .param("tipoOperacion", "MODIFICACION")
                        .param("usuarioId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "docente1", roles = {"DOCENTE"})
    void consultarHistorial_Docente_Retorna403() throws Exception {
        mockMvc.perform(get("/api/cambios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void consultarHistorial_FechaInvalida_Retorna400() throws Exception {
        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido"));

        mockMvc.perform(get("/api/cambios")
                        .param("fechaDesde", "2026-05-10")
                        .param("fechaHasta", "2026-05-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void exportarHistorial_PdfSinRegistrosConAcceptPdf_Retorna200() throws Exception {
        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/cambios/exportar")
                        .param("formato", "pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void exportarHistorial_XlsxSinRegistrosConAcceptXlsx_Retorna200() throws Exception {
        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/cambios/exportar")
                        .param("formato", "xlsx")
                        .accept(ExportFormat.XLSX.getMediaType()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExportFormat.XLSX.getMediaType()));
    }

    @Test
    @WithMockUser(username = "admin_global", roles = {"ADMIN_GLOBAL"})
    void exportarHistorial_FormatoInvalidoConAcceptPdf_Retorna400JsonYNo406() throws Exception {
        when(cambioService.consultarHistorial(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/cambios/exportar")
                        .param("formato", "csv")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }
}

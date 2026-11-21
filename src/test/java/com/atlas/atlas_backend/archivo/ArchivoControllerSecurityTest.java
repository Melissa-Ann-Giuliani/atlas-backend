package com.atlas.atlas_backend.archivo;

import com.atlas.atlas_backend.security.CustomUserDetailsService;
import com.atlas.atlas_backend.security.JwtAuthenticationFilter;
import com.atlas.atlas_backend.security.JwtUtil;
import com.atlas.atlas_backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ArchivoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArchivoService archivoService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "docente")
    void authenticatedUserCanDownloadFile(@TempDir Path tempDir) throws Exception {
        Path pdf = tempDir.resolve("abc123.pdf");
        Files.writeString(pdf, "%PDF-1.4 test");
        when(archivoService.cargarArchivo("abc123.pdf")).thenReturn(pdf);

        mockMvc.perform(get("/api/archivos/descargar/abc123.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"abc123.pdf\""));
    }

    @Test
    @WithMockUser(username = "docente")
    void authenticatedUserCanListTramiteFiles() throws Exception {
        when(archivoService.obtenerArchivosPorTramite("DESIGNACION", 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/archivos/tramite/DESIGNACION/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "docente")
    void authenticatedUserCanUploadFile() throws Exception {
        Archivo saved = new Archivo();
        saved.setNombreEncriptado("abc123.pdf");
        when(archivoService.guardarArchivo(any(), eq("DESIGNACION"), eq(1))).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile(
                "file", "resolucion.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4".getBytes());

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file)
                        .param("tipoTramite", "DESIGNACION")
                        .param("tramiteId", "1"))
                .andExpect(status().isCreated());
    }

    @Test
    void unauthenticatedUserIsForbiddenOnDownload() throws Exception {
        mockMvc.perform(get("/api/archivos/descargar/abc123.pdf"))
                .andExpect(status().isForbidden());
    }
}

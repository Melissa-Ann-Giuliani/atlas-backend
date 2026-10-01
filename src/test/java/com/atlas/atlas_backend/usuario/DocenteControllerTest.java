package com.atlas.atlas_backend.usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocenteController.class)
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
public class DocenteControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DocenteService docenteService;

        @MockBean
        private com.atlas.atlas_backend.auth.AuthService authService; // Needed for Security context

        @MockBean
        private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

        @MockBean
        private com.atlas.atlas_backend.security.CustomUserDetailsService customUserDetailsService;

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN_GLOBAL" })
        @SuppressWarnings("null")
        void getActiveTeachers_Returns200AndPageOfTeachers() throws Exception {
                DocenteListDTO dto = new DocenteListDTO() {
                        public Integer getUsuarioId() { return 1; }
                        public String getNombre() { return "Juan"; }
                        public String getApellido() { return "Perez"; }
                        public String getOrigen() { return null; }
                        public String getUnidad() { return null; }
                        public String getCategoria() { return null; }
                        public String getDedicacion() { return null; }
                        public String getCaracter() { return null; }
                        public String getEstado() { return null; }
                };

                Page<DocenteListDTO> mockPage = new PageImpl<>(List.of(dto));

                when(docenteService.getActiveTeachers(anyString(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(mockPage);

                mockMvc.perform(get("/api/docentes")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].usuarioId").value(1))
                                .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
                                .andExpect(jsonPath("$.content[0].apellido").value("Perez"));
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN_GLOBAL" })
        void getActiveTeachers_Returns500OnException() throws Exception {
                when(docenteService.getActiveTeachers(anyString(), any(), any(), any(), any(Pageable.class)))
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/api/docentes"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN_GLOBAL" })
        void getTeacherDetails_Returns200AndTeacherProfile() throws Exception {
                DocenteDetailsDTO dto = new DocenteDetailsDTO();
                DocenteDetailsDTO.PersonalData personalData = new DocenteDetailsDTO.PersonalData();
                personalData.setNombre("Ana");
                personalData.setApellido("Gomez");
                dto.setDatosPersonales(personalData);

                when(docenteService.getTeacherProfile(anyInt())).thenReturn(dto);

                mockMvc.perform(get("/api/docentes/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.datosPersonales.nombre").value("Ana"))
                                .andExpect(jsonPath("$.datosPersonales.apellido").value("Gomez"));
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN_GLOBAL" })
        void getTeacherDetails_Returns404OnException() throws Exception {
                when(docenteService.getTeacherProfile(anyInt())).thenThrow(new RuntimeException("Teacher not found"));

                mockMvc.perform(get("/api/docentes/999"))
                                .andExpect(status().isNotFound());
        }
}

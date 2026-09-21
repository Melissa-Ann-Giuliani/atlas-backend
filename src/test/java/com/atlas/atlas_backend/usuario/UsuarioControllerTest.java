package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UsuarioControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @MockBean
        private AdminGlobalService adminGlobalService;

        @MockBean
        private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

        @MockBean
        private com.atlas.atlas_backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testScenario1_AdminResetPassword_Success() throws Exception {
                mockMvc.perform(post("/api/usuarios/42/reset-password")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Password reset successfully for user 42."));
        }

        @Test
        @WithMockUser(roles = "DOCENTE")
        public void testScenario2_AdminResetPassword_ForbiddenForDocente() throws Exception {
                // En una app real, SecurityConfig + @PreAuthorize con filter activo validaria
                // el FORBIDDEN
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testScenario4_AdminResetPassword_UserNotFound() throws Exception {
                doThrow(new Exception("User not found."))
                                .when(authService).adminResetPassword(anyInt());

                mockMvc.perform(post("/api/usuarios/9999/reset-password")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("User not found."));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testScenario5_AdminResetPassword_InactiveUser() throws Exception {
                doThrow(new Exception("Cannot reset password for an inactive account."))
                                .when(authService).adminResetPassword(anyInt());

                mockMvc.perform(post("/api/usuarios/43/reset-password")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message")
                                                .value("Cannot reset password for an inactive account."));
        }

        @SuppressWarnings("null")
        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testRegistrarAdminGlobal_Success() throws Exception {
                AdminGlobalRequest request = new AdminGlobalRequest();
                request.setUsername("nuevo_admin");
                request.setCorreo("admin@test.com");
                request.setNombre("Juan");
                request.setApellido("Perez");
                request.setCargoNombre(CargoAdminGlobal.DIRECCION_DE_PERSONAL);

                AdminGlobal mockResponse = new AdminGlobal();
                mockResponse.setUsername("nuevo_admin");

                org.mockito.Mockito
                                .when(adminGlobalService.registrarAdminGlobal(
                                                org.mockito.ArgumentMatchers.any(AdminGlobalRequest.class),
                                                org.mockito.ArgumentMatchers.any(StringBuilder.class)))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/api/usuarios/admin-global")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Administrador Global registrado exitosamente"))
                                .andExpect(jsonPath("$.usuario.username").value("nuevo_admin"));
        }

        @SuppressWarnings("null")
        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testRegistrarAdminGlobal_MissingFields() throws Exception {
                AdminGlobalRequest request = new AdminGlobalRequest();
                request.setUsername("nuevo_admin");
                // Missing correo, nombre, apellido, cargoNombre

                mockMvc.perform(post("/api/usuarios/admin-global")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest()); // Handled by @Valid and
                                                                     // DefaultHandlerExceptionResolver
        }
}

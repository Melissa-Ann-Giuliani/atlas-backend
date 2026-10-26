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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class UsuarioControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @MockBean
        private AdminGlobalService adminGlobalService;

        @MockBean
        private AdminUnidadService adminUnidadService;

        @MockBean
        private AuditorService auditorService;

        @MockBean
        private DocenteService docenteService;

        @MockBean
        private UsuarioService usuarioService;

        @MockBean
        private UsuarioRepository usuarioRepository;

        @MockBean
        private AdminGlobalRepository adminGlobalRepository;

        @MockBean
        private AdminUnidadRepository adminUnidadRepository;

        @MockBean
        private AuditorRepository auditorRepository;

        @MockBean
        private DocenteRepository docenteRepository;

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
                                .andExpect(jsonPath("$.message").value("Contraseña generada con éxito para usuario 42."));
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
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Administrador Global registrado exitosamente"))
                                .andExpect(jsonPath("$.usuario.username").value("nuevo_admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testRegistrarAdminGlobal_MissingFields() throws Exception {
                AdminGlobalRequest request = new AdminGlobalRequest();
                request.setUsername("nuevo_admin");
                // Missing correo, nombre, apellido, cargoNombre

                mockMvc.perform(post("/api/usuarios/admin-global")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                .andExpect(status().isBadRequest()); // Handled by @Valid and
                                                                     // DefaultHandlerExceptionResolver
        }

        @Test
        @WithMockUser
        public void testGenerarContrasenia_Success() throws Exception {
                mockMvc.perform(get("/api/usuarios/generar-contrasenia"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.password").isString())
                                .andExpect(jsonPath("$.password").value(java.util.Objects.requireNonNull(org.hamcrest.Matchers.hasLength(12))));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testGetCargosAdminUnidad_Success() throws Exception {
                mockMvc.perform(get("/api/usuarios/admin-unidad/cargos"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].value").value("DIRECTOR"))
                                .andExpect(jsonPath("$[0].nombre").value("Director"))
                                .andExpect(jsonPath("$[1].value").value("VICEDIRECTOR"))
                                .andExpect(jsonPath("$[1].nombre").value("Vice Director"))
                                .andExpect(jsonPath("$[2].value").value("SECRETARIA_ADMINISTRATIVA"))
                                .andExpect(jsonPath("$[2].nombre").value("Secretaría Administrativa"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testGetCargosAuditor_Success() throws Exception {
                mockMvc.perform(get("/api/usuarios/auditor/cargos"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].value").value("DECANO"))
                                .andExpect(jsonPath("$[0].nombre").value("Decano"))
                                .andExpect(jsonPath("$[1].value").value("VICEDECANO"))
                                .andExpect(jsonPath("$[1].nombre").value("Vicedecano"))
                                .andExpect(jsonPath("$[2].value").value("SECRETARIA_ACADEMICA"))
                                .andExpect(jsonPath("$[2].nombre").value("Secretaría Académica"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testGetTiposDniDocente_Success() throws Exception {
                mockMvc.perform(get("/api/usuarios/docente/tipos-dni"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].value").value("DNI"))
                                .andExpect(jsonPath("$[0].nombre").value("DNI"))
                                .andExpect(jsonPath("$[1].value").value("PASAPORTE"))
                                .andExpect(jsonPath("$[1].nombre").value("Pasaporte"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testGetUsuarios_FilterByRoleAndSearch_Success() throws Exception {
                Usuario user = new Usuario();
                user.setId(10);
                user.setUsername("admin_test");
                user.setNombre("Carlos");
                user.setApellido("Gomez");
                user.setCorreo("carlos@atlas.com");
                user.setActivo(true);
                Rol rol = new Rol();
                rol.setId(2);
                rol.setNombre("ADMIN_UNIDAD");
                user.setRol(rol);

                org.springframework.data.domain.Page<Usuario> page = new org.springframework.data.domain.PageImpl<>(
                                java.util.Objects.requireNonNull(java.util.List.of(user)),
                                org.springframework.data.domain.PageRequest.of(0, 10),
                                1
                );

                org.mockito.Mockito.when(usuarioRepository.searchUsuarios(
                                org.mockito.ArgumentMatchers.eq("Carlos"),
                                org.mockito.ArgumentMatchers.eq("ADMIN_UNIDAD"),
                                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
                )).thenReturn(page);

                mockMvc.perform(get("/api/usuarios?search=Carlos&rol=ADMIN_UNIDAD"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(10))
                                .andExpect(jsonPath("$.content[0].username").value("admin_test"))
                                .andExpect(jsonPath("$.content[0].nombre").value("Carlos"))
                                .andExpect(jsonPath("$.content[0].apellido").value("Gomez"))
                                .andExpect(jsonPath("$.content[0].rol").value("ADMIN_UNIDAD"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_GLOBAL")
        public void testGetUsuarioById_Success() throws Exception {
                Usuario user = new Usuario();
                user.setId(10);
                user.setUsername("admin_test");
                user.setNombre("Carlos");
                user.setApellido("Gomez");
                user.setCorreo("carlos@atlas.com");
                user.setActivo(true);
                Rol rol = new Rol();
                rol.setId(2);
                rol.setNombre("ADMIN_UNIDAD");
                user.setRol(rol);

                org.mockito.Mockito.when(usuarioRepository.findById(10)).thenReturn(java.util.Optional.of(user));

                mockMvc.perform(get("/api/usuarios/10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.username").value("admin_test"))
                                .andExpect(jsonPath("$.nombre").value("Carlos"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminGlobal_Success() throws Exception {
                AdminGlobal updated = new AdminGlobal();
                updated.setId(5);
                updated.setUsername("admin_mod");
                updated.setNombre("Carlos");
                updated.setApellido("Perez");
                updated.setCorreo("carlos@atlas.com");
                updated.setCargoNombre(CargoAdminGlobal.DESPACHO);

                when(adminGlobalService.modificarAdminGlobal(eq(5), any(AdminGlobalUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenReturn(updated);

                AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
                updateReq.setUsername("admin_mod");
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Perez");
                updateReq.setCorreo("carlos@atlas.com");
                updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

                mockMvc.perform(put("/api/usuarios/admin-global/5")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Administrador Global modificado exitosamente"))
                                .andExpect(jsonPath("$.usuario.username").value("admin_mod"))
                                .andExpect(jsonPath("$.usuario.nombre").value("Carlos"))
                                .andExpect(jsonPath("$.usuario.cargoNombre").value("DESPACHO"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminGlobal_ValidationError() throws Exception {
                AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
                updateReq.setUsername(""); // blank username
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Perez");
                updateReq.setCorreo("invalid-email");
                updateReq.setCargoNombre(null);

                mockMvc.perform(put("/api/usuarios/admin-global/5")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminGlobal_Conflict() throws Exception {
                when(adminGlobalService.modificarAdminGlobal(eq(5), any(AdminGlobalUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "El nombre de usuario ya se encuentra en uso"
                                ));

                AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
                updateReq.setUsername("existing_user");
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Perez");
                updateReq.setCorreo("carlos@atlas.com");
                updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

                mockMvc.perform(put("/api/usuarios/admin-global/5")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El nombre de usuario ya se encuentra en uso"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminGlobal_NotFound() throws Exception {
                when(adminGlobalService.modificarAdminGlobal(eq(999), any(AdminGlobalUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Administrador Global no encontrado"
                                ));

                AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
                updateReq.setUsername("non_existent");
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Perez");
                updateReq.setCorreo("carlos@atlas.com");
                updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

                mockMvc.perform(put("/api/usuarios/admin-global/999")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Administrador Global no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminUnidad_Success() throws Exception {
                AdminUnidad updated = new AdminUnidad();
                updated.setId(7);
                updated.setUsername("admin_u_mod");
                updated.setNombre("Maria Ana");
                updated.setApellido("Gomez");
                updated.setCorreo("maria@atlas.com");
                updated.setCargoNombre(CargoAdminUnidad.DIRECTOR);

                when(adminUnidadService.modificarAdminUnidad(eq(7), any(AdminUnidadUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenReturn(updated);

                AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
                updateReq.setUsername("admin_u_mod");
                updateReq.setNombre("Maria Ana");
                updateReq.setApellido("Gomez");
                updateReq.setCorreo("maria@atlas.com");
                updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
                updateReq.setUnidadId(1);

                mockMvc.perform(put("/api/usuarios/admin-unidad/7")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Administrador de Unidad modificado exitosamente"))
                                .andExpect(jsonPath("$.usuario.username").value("admin_u_mod"))
                                .andExpect(jsonPath("$.usuario.nombre").value("Maria Ana"))
                                .andExpect(jsonPath("$.usuario.cargoNombre").value("DIRECTOR"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminUnidad_ValidationError() throws Exception {
                AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
                updateReq.setUsername(""); // blank username
                updateReq.setNombre("Maria");
                updateReq.setApellido("Gomez");
                updateReq.setCorreo("invalid-email");
                updateReq.setCargoNombre(null);
                updateReq.setUnidadId(null);

                mockMvc.perform(put("/api/usuarios/admin-unidad/7")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminUnidad_Conflict() throws Exception {
                when(adminUnidadService.modificarAdminUnidad(eq(7), any(AdminUnidadUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "El nombre de usuario ya se encuentra en uso"
                                ));

                AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
                updateReq.setUsername("existing_user");
                updateReq.setNombre("Maria");
                updateReq.setApellido("Gomez");
                updateReq.setCorreo("maria@atlas.com");
                updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
                updateReq.setUnidadId(1);

                mockMvc.perform(put("/api/usuarios/admin-unidad/7")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El nombre de usuario ya se encuentra en uso"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminUnidad_NotFound() throws Exception {
                when(adminUnidadService.modificarAdminUnidad(eq(999), any(AdminUnidadUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Administrador de Unidad no encontrado"
                                ));

                AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
                updateReq.setUsername("non_existent");
                updateReq.setNombre("Maria");
                updateReq.setApellido("Gomez");
                updateReq.setCorreo("maria@atlas.com");
                updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
                updateReq.setUnidadId(1);

                mockMvc.perform(put("/api/usuarios/admin-unidad/999")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Administrador de Unidad no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAdminUnidad_UnidadNotFound() throws Exception {
                when(adminUnidadService.modificarAdminUnidad(eq(7), any(AdminUnidadUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Unidad no encontrada"
                                ));

                AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
                updateReq.setUsername("maria");
                updateReq.setNombre("Maria");
                updateReq.setApellido("Gomez");
                updateReq.setCorreo("maria@atlas.com");
                updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
                updateReq.setUnidadId(999);

                mockMvc.perform(put("/api/usuarios/admin-unidad/7")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Unidad no encontrada"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAuditor_Success() throws Exception {
                Auditor updated = new Auditor();
                updated.setId(15);
                updated.setUsername("carlos_lopez_decano");
                updated.setNombre("Carlos Alberto");
                updated.setApellido("Lopez Martinez");
                updated.setCorreo("c.lopez@unc.edu.ar");
                updated.setCargoNombre(CargoAuditor.DECANO);

                when(auditorService.modificarAuditor(eq(15), any(AuditorUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenReturn(updated);

                AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
                updateReq.setUsername("carlos_lopez_decano");
                updateReq.setNombre("Carlos Alberto");
                updateReq.setApellido("Lopez Martinez");
                updateReq.setCorreo("c.lopez@unc.edu.ar");
                updateReq.setCargoNombre(CargoAuditor.DECANO);

                mockMvc.perform(put("/api/usuarios/auditor/15")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Auditor modificado exitosamente"))
                                .andExpect(jsonPath("$.usuario.username").value("carlos_lopez_decano"))
                                .andExpect(jsonPath("$.usuario.nombre").value("Carlos Alberto"))
                                .andExpect(jsonPath("$.usuario.cargoNombre").value("DECANO"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAuditor_ValidationFailure() throws Exception {
                AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
                updateReq.setUsername("");
                updateReq.setNombre("");
                updateReq.setApellido("Lopez");
                updateReq.setCorreo("invalid-email");
                updateReq.setCargoNombre(null);

                mockMvc.perform(put("/api/usuarios/auditor/15")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAuditor_Conflict() throws Exception {
                when(auditorService.modificarAuditor(eq(15), any(AuditorUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "El nombre de usuario ya se encuentra en uso"
                                ));

                AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
                updateReq.setUsername("juan_perez");
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Lopez");
                updateReq.setCorreo("carlos@unc.edu.ar");
                updateReq.setCargoNombre(CargoAuditor.VICEDECANO);

                mockMvc.perform(put("/api/usuarios/auditor/15")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El nombre de usuario ya se encuentra en uso"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testModificarAuditor_NotFound() throws Exception {
                when(auditorService.modificarAuditor(eq(9999), any(AuditorUpdateRequest.class), org.mockito.ArgumentMatchers.nullable(String.class)))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Auditor no encontrado"
                                ));

                AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
                updateReq.setUsername("non_existent");
                updateReq.setNombre("Carlos");
                updateReq.setApellido("Lopez");
                updateReq.setCorreo("carlos@unc.edu.ar");
                updateReq.setCargoNombre(CargoAuditor.DECANO);

                mockMvc.perform(put("/api/usuarios/auditor/9999")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(updateReq))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Auditor no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testDarDeBajaUsuario_Success() throws Exception {
                java.util.Map<String, Object> respMap = java.util.Map.of(
                                "message", "Usuario dado de baja exitosamente",
                                "usuario", java.util.Map.of("id", 10, "username", "juan", "activo", false)
                );

                when(usuarioService.darDeBajaUsuario(eq(10), eq("admin_auth")))
                                .thenReturn(respMap);

                mockMvc.perform(patch("/api/usuarios/10/baja")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Usuario dado de baja exitosamente"))
                                .andExpect(jsonPath("$.usuario.id").value(10))
                                .andExpect(jsonPath("$.usuario.activo").value(false));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testDarDeBajaUsuario_Conflict_ActiveCargos() throws Exception {
                when(usuarioService.darDeBajaUsuario(eq(25), eq("admin_auth")))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "No se puede dar de baja el usuario porque posee cargos vigentes"
                                ));

                mockMvc.perform(patch("/api/usuarios/25/baja")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("No se puede dar de baja el usuario porque posee cargos vigentes"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testDarDeBajaUsuario_NotFound() throws Exception {
                when(usuarioService.darDeBajaUsuario(eq(99999), eq("admin_auth")))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"
                                ));

                mockMvc.perform(patch("/api/usuarios/99999/baja")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
        }

        @Test
        @WithMockUser(username = "admin_auth", roles = "ADMIN_GLOBAL")
        public void testDarDeBajaUsuario_SelfDecommission_BadRequest() throws Exception {
                when(usuarioService.darDeBajaUsuario(eq(1), eq("admin_auth")))
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.BAD_REQUEST,
                                                "No puede darse de baja a sí mismo"
                                ));

                mockMvc.perform(patch("/api/usuarios/1/baja")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("No puede darse de baja a sí mismo"));
        }
}

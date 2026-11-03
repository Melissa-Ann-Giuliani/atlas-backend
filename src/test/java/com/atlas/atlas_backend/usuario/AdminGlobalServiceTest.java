package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import org.springframework.lang.NonNull;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminGlobalServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminGlobalRepository adminGlobalRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private com.atlas.atlas_backend.cambio.CambioService cambioService;

    @InjectMocks
    private AdminGlobalService adminGlobalService;

    @NonNull
    private static AdminGlobal anyAdminGlobal() {
        AdminGlobal a = any(AdminGlobal.class);
        return a != null ? a : new AdminGlobal();
    }

    private AdminGlobalRequest request;

    @BeforeEach
    public void setup() {
        request = new AdminGlobalRequest();
        request.setUsername("admin_test");
        request.setCorreo("test@test.com");
        request.setNombre("Test");
        request.setApellido("User");
        request.setCargoNombre(CargoAdminGlobal.DESPACHO);
    }

    @Test
    public void testRegistrarAdminGlobal_Success() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Administrador Global");
        when(rolRepository.findByNombre("Administrador Global")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminGlobal savedUser = new AdminGlobal();
        savedUser.setUsername("admin_test");
        when(adminGlobalRepository.save(anyAdminGlobal())).thenReturn(savedUser);

        StringBuilder warningMessage = new StringBuilder();
        AdminGlobal result = adminGlobalService.registrarAdminGlobal(request, warningMessage);

        assertNotNull(result);
        assertEquals("admin_test", result.getUsername());
        verify(emailService, times(1)).sendWelcomeAdminGlobal(any(), anyString());
        assertEquals(0, warningMessage.length());
    }

    @Test
    public void testRegistrarAdminGlobal_EmailFailureReturnsWarning() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Administrador Global");
        when(rolRepository.findByNombre("Administrador Global")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminGlobal savedUser = new AdminGlobal();
        savedUser.setUsername("admin_test");
        when(adminGlobalRepository.save(anyAdminGlobal())).thenReturn(savedUser);

        doThrow(new RuntimeException("SMTP Error")).when(emailService).sendWelcomeAdminGlobal(any(), anyString());

        StringBuilder warningMessage = new StringBuilder();
        AdminGlobal result = adminGlobalService.registrarAdminGlobal(request, warningMessage);

        assertNotNull(result);
        assertTrue(warningMessage.toString().contains("falló el envío del correo"));
    }

    @Test
    public void testRegistrarAdminGlobal_UsernameExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(new Usuario()));

        Exception exception = assertThrows(Exception.class, () -> {
            adminGlobalService.registrarAdminGlobal(request, new StringBuilder());
        });

        assertEquals("Username ya existe", exception.getMessage());
        verify(adminGlobalRepository, never()).save(anyAdminGlobal());
    }

    // ==================== Tests for modificarAdminGlobal ====================

    @Test
    public void testModificarAdminGlobal_Success() {
        Usuario authUser = new Usuario();
        authUser.setId(1);
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminGlobal existing = new AdminGlobal();
        existing.setId(5);
        existing.setUsername("old_username");
        existing.setNombre("Juan");
        existing.setApellido("Perez");
        existing.setCorreo("juan.perez@unc.edu.ar");
        existing.setCargoNombre(CargoAdminGlobal.DIRECCION_DE_PERSONAL);
        when(adminGlobalRepository.findById(5)).thenReturn(Optional.of(existing));

        when(usuarioRepository.findByUsername("new_username")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreoAndIdNot("new.email@unc.edu.ar", 5)).thenReturn(false);
        when(adminGlobalRepository.save(anyAdminGlobal())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
        updateReq.setUsername("new_username");
        updateReq.setNombre("Juan Carlos");
        updateReq.setApellido("Perez Gomez");
        updateReq.setCorreo("new.email@unc.edu.ar");
        updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

        AdminGlobal result = adminGlobalService.modificarAdminGlobal(5, updateReq, "auth_admin");

        assertNotNull(result);
        assertEquals("new_username", result.getUsername());
        assertEquals("Juan Carlos", result.getNombre());
        assertEquals("Perez Gomez", result.getApellido());
        assertEquals("new.email@unc.edu.ar", result.getCorreo());
        assertEquals(CargoAdminGlobal.DESPACHO, result.getCargoNombre());

        verify(cambioService).registrarCambio("admin_globales", "username", "old_username", "new_username", authUser);
        verify(cambioService).registrarCambio("admin_globales", "nombre", "Juan", "Juan Carlos", authUser);
        verify(cambioService).registrarCambio("admin_globales", "apellido", "Perez", "Perez Gomez", authUser);
        verify(cambioService).registrarCambio("admin_globales", "correo", "juan.perez@unc.edu.ar", "new.email@unc.edu.ar", authUser);
        verify(cambioService).registrarCambio("admin_globales", "cargo_ag_nombre", CargoAdminGlobal.DIRECCION_DE_PERSONAL, CargoAdminGlobal.DESPACHO, authUser);
        verify(adminGlobalRepository).save(existing);
    }

    @Test
    public void testModificarAdminGlobal_NotFound() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));
        when(adminGlobalRepository.findById(999)).thenReturn(Optional.empty());

        AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
        updateReq.setUsername("user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("email@test.com");
        updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminGlobalService.modificarAdminGlobal(999, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("Administrador Global no encontrado"));
    }

    @Test
    public void testModificarAdminGlobal_UsernameConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminGlobal existing = new AdminGlobal();
        existing.setId(5);
        existing.setUsername("old_username");
        existing.setCorreo("test@test.com");
        when(adminGlobalRepository.findById(5)).thenReturn(Optional.of(existing));

        Usuario otherUser = new Usuario();
        otherUser.setId(10);
        otherUser.setUsername("duplicate_username");
        when(usuarioRepository.findByUsername("duplicate_username")).thenReturn(Optional.of(otherUser));

        AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
        updateReq.setUsername("duplicate_username");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("test@test.com");
        updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminGlobalService.modificarAdminGlobal(5, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("El nombre de usuario ya se encuentra en uso"));
        verify(adminGlobalRepository, never()).save(anyAdminGlobal());
    }

    @Test
    public void testModificarAdminGlobal_EmailConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminGlobal existing = new AdminGlobal();
        existing.setId(5);
        existing.setUsername("admin_user");
        existing.setCorreo("old@test.com");
        when(adminGlobalRepository.findById(5)).thenReturn(Optional.of(existing));

        when(usuarioRepository.existsByCorreoAndIdNot("taken@test.com", 5)).thenReturn(true);

        AdminGlobalUpdateRequest updateReq = new AdminGlobalUpdateRequest();
        updateReq.setUsername("admin_user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("taken@test.com");
        updateReq.setCargoNombre(CargoAdminGlobal.DESPACHO);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminGlobalService.modificarAdminGlobal(5, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("El correo electrónico ya se encuentra en uso"));
        verify(adminGlobalRepository, never()).save(anyAdminGlobal());
    }
}

package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.cambio.CambioService;
import com.atlas.atlas_backend.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditorServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditorRepository auditorRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private CambioService cambioService;

    @InjectMocks
    private AuditorService auditorService;

    @NonNull
    private static Auditor anyAuditor() {
        Auditor a = any(Auditor.class);
        return a != null ? a : new Auditor();
    }

    private AuditorRequest request;

    @BeforeEach
    public void setup() {
        request = new AuditorRequest();
        request.setUsername("auditor_test");
        request.setCorreo("test@test.com");
        request.setNombre("Test");
        request.setApellido("User");
        request.setCargoNombre(CargoAuditor.VICEDECANO);
    }

    @Test
    public void testRegistrarAuditor_Success() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Auditor");
        when(rolRepository.findByNombre("Auditor")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Auditor savedUser = new Auditor();
        savedUser.setUsername("auditor_test");
        when(auditorRepository.save(anyAuditor())).thenReturn(savedUser);

        StringBuilder warningMessage = new StringBuilder();
        Auditor result = auditorService.registrarAuditor(request, warningMessage);

        assertNotNull(result);
        assertEquals("auditor_test", result.getUsername());
        verify(emailService, times(1)).sendWelcomeAuditor(any(), anyString());
        assertEquals(0, warningMessage.length());
    }

    @Test
    public void testRegistrarAuditor_EmailFailureReturnsWarning() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Auditor");
        when(rolRepository.findByNombre("Auditor")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Auditor savedUser = new Auditor();
        savedUser.setUsername("auditor_test");
        when(auditorRepository.save(anyAuditor())).thenReturn(savedUser);

        doThrow(new RuntimeException("SMTP error")).when(emailService).sendWelcomeAuditor(any(), anyString());

        StringBuilder warningMessage = new StringBuilder();
        Auditor result = auditorService.registrarAuditor(request, warningMessage);

        assertNotNull(result);
        assertTrue(warningMessage.toString().contains("falló el envío del correo"));
    }

    @Test
    public void testRegistrarAuditor_UsernameDuplicate() {
        when(usuarioRepository.findByUsername("auditor_test")).thenReturn(Optional.of(new Usuario()));

        StringBuilder warningMessage = new StringBuilder();
        Exception ex = assertThrows(Exception.class, () -> auditorService.registrarAuditor(request, warningMessage));
        assertTrue(ex.getMessage().contains("Username ya existe"));
    }

    @Test
    public void testRegistrarAuditor_EmailDuplicate() {
        when(usuarioRepository.findByUsername("auditor_test")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("test@test.com")).thenReturn(true);

        StringBuilder warningMessage = new StringBuilder();
        Exception ex = assertThrows(Exception.class, () -> auditorService.registrarAuditor(request, warningMessage));
        assertTrue(ex.getMessage().contains("Correo ya existe"));
    }

    @Test
    public void testModificarAuditor_Success() {
        Usuario authUser = new Usuario();
        authUser.setId(1);
        authUser.setUsername("auth_admin");

        Auditor existing = new Auditor();
        existing.setId(15);
        existing.setUsername("old_username");
        existing.setNombre("Carlos");
        existing.setApellido("Lopez");
        existing.setCorreo("carlos.lopez@unc.edu.ar");
        existing.setCargoNombre(CargoAuditor.VICEDECANO);

        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));
        when(auditorRepository.findById(15)).thenReturn(Optional.of(existing));
        when(usuarioRepository.findByUsername("new_username")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreoAndIdNot("c.lopez@unc.edu.ar", 15)).thenReturn(false);
        when(auditorRepository.save(anyAuditor())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("new_username");
        updateReq.setNombre("Carlos Alberto");
        updateReq.setApellido("Lopez Martinez");
        updateReq.setCorreo("c.lopez@unc.edu.ar");
        updateReq.setCargoNombre(CargoAuditor.DECANO);

        Auditor result = auditorService.modificarAuditor(15, updateReq, "auth_admin");

        assertNotNull(result);
        assertEquals("new_username", result.getUsername());
        assertEquals("Carlos Alberto", result.getNombre());
        assertEquals("Lopez Martinez", result.getApellido());
        assertEquals("c.lopez@unc.edu.ar", result.getCorreo());
        assertEquals(CargoAuditor.DECANO, result.getCargoNombre());

        verify(cambioService).registrarCambio("auditores", "username", "old_username", "new_username", authUser);
        verify(cambioService).registrarCambio("auditores", "nombre", "Carlos", "Carlos Alberto", authUser);
        verify(cambioService).registrarCambio("auditores", "apellido", "Lopez", "Lopez Martinez", authUser);
        verify(cambioService).registrarCambio("auditores", "correo", "carlos.lopez@unc.edu.ar", "c.lopez@unc.edu.ar", authUser);
        verify(cambioService).registrarCambio("auditores", "cargo_au_nombre", CargoAuditor.VICEDECANO, CargoAuditor.DECANO, authUser);
        verify(auditorRepository).save(existing);
    }

    @Test
    public void testModificarAuditor_PartialUpdateNoRedundantAudit() {
        Usuario authUser = new Usuario();
        authUser.setId(1);
        authUser.setUsername("auth_admin");

        Auditor existing = new Auditor();
        existing.setId(15);
        existing.setUsername("auditor_unc");
        existing.setNombre("Carlos");
        existing.setApellido("Lopez");
        existing.setCorreo("carlos.lopez@unc.edu.ar");
        existing.setCargoNombre(CargoAuditor.VICEDECANO);

        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));
        when(auditorRepository.findById(15)).thenReturn(Optional.of(existing));
        when(usuarioRepository.existsByCorreoAndIdNot("carlos.nuevo@unc.edu.ar", 15)).thenReturn(false);
        when(auditorRepository.save(anyAuditor())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("auditor_unc");
        updateReq.setNombre("Carlos");
        updateReq.setApellido("Lopez");
        updateReq.setCorreo("carlos.nuevo@unc.edu.ar");
        updateReq.setCargoNombre(CargoAuditor.VICEDECANO);

        Auditor result = auditorService.modificarAuditor(15, updateReq, "auth_admin");

        assertNotNull(result);
        assertEquals("carlos.nuevo@unc.edu.ar", result.getCorreo());

        verify(cambioService).registrarCambio("auditores", "username", "auditor_unc", "auditor_unc", authUser);
        verify(cambioService).registrarCambio("auditores", "nombre", "Carlos", "Carlos", authUser);
        verify(cambioService).registrarCambio("auditores", "apellido", "Lopez", "Lopez", authUser);
        verify(cambioService).registrarCambio("auditores", "correo", "carlos.lopez@unc.edu.ar", "carlos.nuevo@unc.edu.ar", authUser);
        verify(cambioService).registrarCambio("auditores", "cargo_au_nombre", CargoAuditor.VICEDECANO, CargoAuditor.VICEDECANO, authUser);
        verify(auditorRepository).save(existing);
    }

    @Test
    public void testModificarAuditor_NotFound() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));
        when(auditorRepository.findById(999)).thenReturn(Optional.empty());

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("email@test.com");
        updateReq.setCargoNombre(CargoAuditor.DECANO);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> auditorService.modificarAuditor(999, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("Auditor no encontrado"));
    }

    @Test
    public void testModificarAuditor_UsernameConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        Auditor existing = new Auditor();
        existing.setId(15);
        existing.setUsername("old_username");
        existing.setCorreo("test@test.com");
        when(auditorRepository.findById(15)).thenReturn(Optional.of(existing));

        Usuario otherUser = new Usuario();
        otherUser.setId(10);
        otherUser.setUsername("juan_perez");
        when(usuarioRepository.findByUsername("juan_perez")).thenReturn(Optional.of(otherUser));

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("juan_perez");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("test@test.com");
        updateReq.setCargoNombre(CargoAuditor.DECANO);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> auditorService.modificarAuditor(15, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("El nombre de usuario ya se encuentra en uso"));
        verify(auditorRepository, never()).save(anyAuditor());
    }

    @Test
    public void testModificarAuditor_EmailConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        Auditor existing = new Auditor();
        existing.setId(15);
        existing.setUsername("auditor_user");
        existing.setCorreo("old@test.com");
        when(auditorRepository.findById(15)).thenReturn(Optional.of(existing));

        when(usuarioRepository.existsByCorreoAndIdNot("juan@unc.edu.ar", 15)).thenReturn(true);

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("auditor_user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("juan@unc.edu.ar");
        updateReq.setCargoNombre(CargoAuditor.DECANO);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> auditorService.modificarAuditor(15, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("El correo electrónico ya se encuentra en uso"));
        verify(auditorRepository, never()).save(anyAuditor());
    }

    @Test
    public void testModificarAuditor_AuthUserNotFound() {
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.empty());

        AuditorUpdateRequest updateReq = new AuditorUpdateRequest();
        updateReq.setUsername("user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("email@test.com");
        updateReq.setCargoNombre(CargoAuditor.DECANO);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> auditorService.modificarAuditor(15, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("Usuario autenticado no encontrado"));
    }
}

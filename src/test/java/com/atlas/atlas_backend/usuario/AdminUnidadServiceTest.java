package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
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
public class AdminUnidadServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UnidadRepository unidadRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private com.atlas.atlas_backend.cambio.CambioService cambioService;

    @InjectMocks
    private AdminUnidadService adminUnidadService;

    @NonNull
    private static AdminUnidad anyAdminUnidad() {
        AdminUnidad a = any(AdminUnidad.class);
        return a != null ? a : new AdminUnidad();
    }

    private AdminUnidadRequest request;

    @BeforeEach
    public void setup() {
        request = new AdminUnidadRequest();
        request.setUsername("admin_unidad_test");
        request.setCorreo("unidad@test.com");
        request.setNombre("Test");
        request.setApellido("Unidad");
        request.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        request.setUnidadId(1);
    }


    @Test
    public void testRegistrarAdminUnidad_Success() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Administrador de Unidad");
        when(rolRepository.findByNombre("Administrador de Unidad")).thenReturn(Optional.of(rol));

        Unidad unidad = new Unidad();
        unidad.setUnidadId(1);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminUnidad savedUser = new AdminUnidad();
        savedUser.setUsername("admin_unidad_test");
        when(adminUnidadRepository.save(anyAdminUnidad())).thenReturn(savedUser);

        StringBuilder warningMessage = new StringBuilder();
        AdminUnidad result = adminUnidadService.registrarAdminUnidad(request, warningMessage);

        assertNotNull(result);
        assertEquals("admin_unidad_test", result.getUsername());
        verify(emailService, times(1)).sendWelcomeAdminUnidad(any(), anyString());
        assertEquals(0, warningMessage.length());
    }

    @Test
    public void testRegistrarAdminUnidad_EmailFailureReturnsWarning() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Administrador de Unidad");
        when(rolRepository.findByNombre("Administrador de Unidad")).thenReturn(Optional.of(rol));

        Unidad unidad = new Unidad();
        unidad.setUnidadId(1);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminUnidad savedUser = new AdminUnidad();
        savedUser.setUsername("admin_unidad_test");
        when(adminUnidadRepository.save(anyAdminUnidad())).thenReturn(savedUser);

        doThrow(new RuntimeException("SMTP Error")).when(emailService).sendWelcomeAdminUnidad(any(), anyString());

        StringBuilder warningMessage = new StringBuilder();
        AdminUnidad result = adminUnidadService.registrarAdminUnidad(request, warningMessage);

        assertNotNull(result);
        assertTrue(warningMessage.toString().contains("falló el envío del correo"));
    }

    @Test
    public void testRegistrarAdminUnidad_UsernameExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(new Usuario()));

        Exception exception = assertThrows(Exception.class, () -> {
            adminUnidadService.registrarAdminUnidad(request, new StringBuilder());
        });

        assertEquals("Username ya existe", exception.getMessage());
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }

    @Test
    public void testRegistrarAdminUnidad_CorreoExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(true);

        Exception exception = assertThrows(Exception.class, () -> {
            adminUnidadService.registrarAdminUnidad(request, new StringBuilder());
        });

        assertEquals("Correo ya existe", exception.getMessage());
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }

    @Test
    public void testRegistrarAdminUnidad_UnidadNotFoundThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Administrador de Unidad");
        when(rolRepository.findByNombre("Administrador de Unidad")).thenReturn(Optional.of(rol));

        when(unidadRepository.findById(1)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            adminUnidadService.registrarAdminUnidad(request, new StringBuilder());
        });

        assertEquals("Unidad no encontrada", exception.getMessage());
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }

    @Test
    public void testModificarAdminUnidad_Success() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        Unidad oldUnidad = new Unidad();
        oldUnidad.setUnidadId(1);
        oldUnidad.setUnidadNombre("Facultad de Ciencias");

        AdminUnidad existing = new AdminUnidad();
        existing.setId(7);
        existing.setUsername("old_user");
        existing.setNombre("Maria");
        existing.setApellido("Gomez");
        existing.setCorreo("maria.gomez@unc.edu.ar");
        existing.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        existing.setUnidad(oldUnidad);

        when(adminUnidadRepository.findById(7)).thenReturn(Optional.of(existing));

        Unidad newUnidad = new Unidad();
        newUnidad.setUnidadId(2);
        newUnidad.setUnidadNombre("Facultad de Artes");
        when(unidadRepository.findById(2)).thenReturn(Optional.of(newUnidad));

        when(usuarioRepository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreoAndIdNot("maria.new@unc.edu.ar", 7)).thenReturn(false);
        when(adminUnidadRepository.save(anyAdminUnidad())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
        updateReq.setUsername("new_user");
        updateReq.setNombre("Maria Ana");
        updateReq.setApellido("Gomez Lopez");
        updateReq.setCorreo("maria.new@unc.edu.ar");
        updateReq.setCargoNombre(CargoAdminUnidad.VICEDIRECTOR);
        updateReq.setUnidadId(2);

        AdminUnidad result = adminUnidadService.modificarAdminUnidad(7, updateReq, "auth_admin");

        assertNotNull(result);
        assertEquals("new_user", result.getUsername());
        assertEquals("Maria Ana", result.getNombre());
        assertEquals("Gomez Lopez", result.getApellido());
        assertEquals("maria.new@unc.edu.ar", result.getCorreo());
        assertEquals(CargoAdminUnidad.VICEDIRECTOR, result.getCargoNombre());
        assertEquals(2, result.getUnidad().getUnidadId());

        verify(cambioService).registrarCambio("admin_de_unidad", "username", "old_user", "new_user", authUser);
        verify(cambioService).registrarCambio("admin_de_unidad", "nombre", "Maria", "Maria Ana", authUser);
        verify(cambioService).registrarCambio("admin_de_unidad", "apellido", "Gomez", "Gomez Lopez", authUser);
        verify(cambioService).registrarCambio("admin_de_unidad", "correo", "maria.gomez@unc.edu.ar", "maria.new@unc.edu.ar", authUser);
        verify(cambioService).registrarCambio("admin_de_unidad", "cargo_ad_nombre", CargoAdminUnidad.DIRECTOR, CargoAdminUnidad.VICEDIRECTOR, authUser);
        verify(cambioService).registrarCambio("admin_de_unidad", "unidad_id", 1, 2, authUser);
        verify(adminUnidadRepository).save(existing);
    }

    @Test
    public void testModificarAdminUnidad_NotFound() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));
        when(adminUnidadRepository.findById(999)).thenReturn(Optional.empty());

        AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
        updateReq.setUsername("user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("email@test.com");
        updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        updateReq.setUnidadId(1);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminUnidadService.modificarAdminUnidad(999, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("Administrador de Unidad no encontrado"));
    }

    @Test
    public void testModificarAdminUnidad_UsernameConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminUnidad existing = new AdminUnidad();
        existing.setId(7);
        existing.setUsername("old_user");
        existing.setCorreo("email@test.com");
        when(adminUnidadRepository.findById(7)).thenReturn(Optional.of(existing));

        Usuario otherUser = new Usuario();
        otherUser.setId(10);
        otherUser.setUsername("duplicate_user");
        when(usuarioRepository.findByUsername("duplicate_user")).thenReturn(Optional.of(otherUser));

        AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
        updateReq.setUsername("duplicate_user");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("email@test.com");
        updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        updateReq.setUnidadId(1);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminUnidadService.modificarAdminUnidad(7, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("El nombre de usuario ya se encuentra en uso"));
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }

    @Test
    public void testModificarAdminUnidad_EmailConflict() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminUnidad existing = new AdminUnidad();
        existing.setId(7);
        existing.setUsername("admin_u");
        existing.setCorreo("old@test.com");
        when(adminUnidadRepository.findById(7)).thenReturn(Optional.of(existing));

        when(usuarioRepository.existsByCorreoAndIdNot("taken@test.com", 7)).thenReturn(true);

        AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
        updateReq.setUsername("admin_u");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("taken@test.com");
        updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        updateReq.setUnidadId(1);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminUnidadService.modificarAdminUnidad(7, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("El correo electrónico ya se encuentra en uso"));
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }

    @Test
    public void testModificarAdminUnidad_UnidadNotFound() {
        Usuario authUser = new Usuario();
        authUser.setUsername("auth_admin");
        when(usuarioRepository.findByUsername("auth_admin")).thenReturn(Optional.of(authUser));

        AdminUnidad existing = new AdminUnidad();
        existing.setId(7);
        existing.setUsername("admin_u");
        existing.setCorreo("test@test.com");
        when(adminUnidadRepository.findById(7)).thenReturn(Optional.of(existing));

        when(unidadRepository.findById(999)).thenReturn(Optional.empty());

        AdminUnidadUpdateRequest updateReq = new AdminUnidadUpdateRequest();
        updateReq.setUsername("admin_u");
        updateReq.setNombre("Name");
        updateReq.setApellido("Lastname");
        updateReq.setCorreo("test@test.com");
        updateReq.setCargoNombre(CargoAdminUnidad.DIRECTOR);
        updateReq.setUnidadId(999);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminUnidadService.modificarAdminUnidad(7, updateReq, "auth_admin")
        );

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(java.util.Objects.requireNonNull(ex.getReason()).contains("Unidad no encontrada"));
        verify(adminUnidadRepository, never()).save(anyAdminUnidad());
    }
}

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

    @InjectMocks
    private AdminUnidadService adminUnidadService;

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
        rol.setNombre("ADMIN_UNIDAD");
        when(rolRepository.findByNombre("ADMIN_UNIDAD")).thenReturn(Optional.of(rol));

        Unidad unidad = new Unidad();
        unidad.setUnidadId(1);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminUnidad savedUser = new AdminUnidad();
        savedUser.setUsername("admin_unidad_test");
        when(adminUnidadRepository.save(any(AdminUnidad.class))).thenReturn(savedUser);

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
        rol.setNombre("ADMIN_UNIDAD");
        when(rolRepository.findByNombre("ADMIN_UNIDAD")).thenReturn(Optional.of(rol));

        Unidad unidad = new Unidad();
        unidad.setUnidadId(1);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminUnidad savedUser = new AdminUnidad();
        savedUser.setUsername("admin_unidad_test");
        when(adminUnidadRepository.save(any(AdminUnidad.class))).thenReturn(savedUser);

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
        verify(adminUnidadRepository, never()).save(any(AdminUnidad.class));
    }

    @Test
    public void testRegistrarAdminUnidad_CorreoExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(true);

        Exception exception = assertThrows(Exception.class, () -> {
            adminUnidadService.registrarAdminUnidad(request, new StringBuilder());
        });

        assertEquals("Correo ya existe", exception.getMessage());
        verify(adminUnidadRepository, never()).save(any(AdminUnidad.class));
    }

    @Test
    public void testRegistrarAdminUnidad_UnidadNotFoundThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("ADMIN_UNIDAD");
        when(rolRepository.findByNombre("ADMIN_UNIDAD")).thenReturn(Optional.of(rol));

        when(unidadRepository.findById(1)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            adminUnidadService.registrarAdminUnidad(request, new StringBuilder());
        });

        assertEquals("Unidad no encontrada", exception.getMessage());
        verify(adminUnidadRepository, never()).save(any(AdminUnidad.class));
    }
}

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

    @InjectMocks
    private AdminGlobalService adminGlobalService;

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
        rol.setNombre("ADMIN_GLOBAL");
        when(rolRepository.findByNombre("ADMIN_GLOBAL")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminGlobal savedUser = new AdminGlobal();
        savedUser.setUsername("admin_test");
        when(adminGlobalRepository.save(any(AdminGlobal.class))).thenReturn(savedUser);

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
        rol.setNombre("ADMIN_GLOBAL");
        when(rolRepository.findByNombre("ADMIN_GLOBAL")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        AdminGlobal savedUser = new AdminGlobal();
        savedUser.setUsername("admin_test");
        when(adminGlobalRepository.save(any(AdminGlobal.class))).thenReturn(savedUser);

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
        verify(adminGlobalRepository, never()).save(any(AdminGlobal.class));
    }
}

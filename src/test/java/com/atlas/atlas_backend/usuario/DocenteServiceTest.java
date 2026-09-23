package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocenteServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DocenteService docenteService;

    private DocenteRequest request;

    @BeforeEach
    public void setup() {
        request = new DocenteRequest();
        request.setUsername("docente_test");
        request.setNombre("Docente");
        request.setApellido("Test");
        request.setDni(12345678L);
        request.setCuil(20123456780L);
        request.setFechaNac(LocalDate.of(1980, 1, 1));
        request.setEmailInstitucional("docente@test.com");
        request.setFechaIngreso(LocalDate.of(2020, 1, 1));
        request.setAntigPrevia(5);
    }

    @Test
    public void testRegistrarDocente_Success() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(docenteRepository.existsByDni(anyLong())).thenReturn(false);
        when(docenteRepository.existsByCuil(anyLong())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("DOCENTE");
        when(rolRepository.findByNombre("DOCENTE")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Docente savedUser = new Docente();
        savedUser.setUsername("docente_test");
        savedUser.setEmailInstitucional("docente@test.com");
        savedUser.setAntigPrevia(5);
        when(docenteRepository.save(any(Docente.class))).thenReturn(savedUser);

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertEquals("docente_test", result.getUsername());
        assertEquals(5, result.getAntigPrevia());
        verify(emailService, times(1)).sendWelcomeDocente(any(), anyString());
        assertEquals(0, warningMessage.length());
    }

    @Test
    public void testRegistrarDocente_EmptySeniorityDefaultsToZero() throws Exception {
        request.setAntigPrevia(null);

        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(docenteRepository.existsByDni(anyLong())).thenReturn(false);
        when(docenteRepository.existsByCuil(anyLong())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("DOCENTE");
        when(rolRepository.findByNombre("DOCENTE")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        when(docenteRepository.save(any(Docente.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertEquals(0, result.getAntigPrevia());
    }

    @Test
    public void testRegistrarDocente_UsernameExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(new Usuario()));

        Exception exception = assertThrows(Exception.class, () -> {
            docenteService.registrarDocente(request, new StringBuilder());
        });

        assertEquals("Username ya existe", exception.getMessage());
        verify(docenteRepository, never()).save(any(Docente.class));
    }
    
    @Test
    public void testRegistrarDocente_EmailFailureReturnsWarning() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(docenteRepository.existsByDni(anyLong())).thenReturn(false);
        when(docenteRepository.existsByCuil(anyLong())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("DOCENTE");
        when(rolRepository.findByNombre("DOCENTE")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Docente savedUser = new Docente();
        savedUser.setUsername("docente_test");
        when(docenteRepository.save(any(Docente.class))).thenReturn(savedUser);

        doThrow(new RuntimeException("SMTP Error")).when(emailService).sendWelcomeDocente(any(), anyString());

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertTrue(warningMessage.toString().contains("falló el envío del correo"));
    }
}

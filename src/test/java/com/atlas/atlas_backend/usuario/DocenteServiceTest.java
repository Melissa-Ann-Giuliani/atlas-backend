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
import org.springframework.lang.NonNull;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private com.atlas.atlas_backend.designacion.DesignacionRepository designacionRepository;

    @Mock
    private com.atlas.atlas_backend.cargo.CargoRepository cargoRepository;

    @Mock
    private com.atlas.atlas_backend.licencia.LicenciaRepository licenciaRepository;

    @Mock
    private com.atlas.atlas_backend.funcion.FuncionRepository funcionRepository;

    @Mock
    private com.atlas.atlas_backend.cambio.CambioService cambioService;

    @Mock
    private com.atlas.atlas_backend.categoria.CategoriaRepository categoriaRepository;

    @Mock
    private com.atlas.atlas_backend.dedicacion.DedicacionRepository dedicacionRepository;

    @Mock
    private com.atlas.atlas_backend.caracter.CaracterRepository caracterRepository;

    @Mock
    private com.atlas.atlas_backend.origen.OrigenRepository origenRepository;

    @Mock
    private com.atlas.atlas_backend.materia.MateriaRepository materiaRepository;

    @InjectMocks
    private DocenteService docenteService;

    @NonNull
    private static Docente anyDocente() {
        Docente d = any(Docente.class);
        return d != null ? d : new Docente();
    }

    private DocenteRequest request;

    @BeforeEach
    public void setup() {
        request = new DocenteRequest();
        request.setUsername("docente_test");
        request.setNombre("Docente");
        request.setApellido("Test");
        request.setDni(12345678L);
        request.setCuil(20123456780L);
        request.setEmailInstitucional("docente@test.com");
        request.setFechaIngreso(LocalDate.of(2020, 1, 1));
        request.setTipoDni("DNI");
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
        rol.setNombre("Docente");
        when(rolRepository.findByNombre("Docente")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Docente savedUser = new Docente();
        savedUser.setUsername("docente_test");
        savedUser.setEmailInstitucional("docente@test.com");
        savedUser.setTipoDni("DNI");
        savedUser.setAntigPrevia(5);
        when(docenteRepository.save(anyDocente())).thenReturn(savedUser);

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertEquals("docente_test", result.getUsername());
        assertEquals("DNI", result.getTipoDni());
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
        rol.setNombre("Docente");
        when(rolRepository.findByNombre("Docente")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        when(docenteRepository.save(anyDocente())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertEquals(0, result.getAntigPrevia());
    }

    @Test
    public void testRegistrarDocente_WithEstado() throws Exception {
        request.setEstado("Inactivo");

        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(docenteRepository.existsByDni(anyLong())).thenReturn(false);
        when(docenteRepository.existsByCuil(anyLong())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Docente");
        when(rolRepository.findByNombre("Docente")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        when(docenteRepository.save(anyDocente())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertEquals("Inactivo", result.getEstado());
    }

    @Test
    public void testRegistrarDocente_UsernameExistsThrowsException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(new Usuario()));

        Exception exception = assertThrows(Exception.class, () -> {
            docenteService.registrarDocente(request, new StringBuilder());
        });

        assertEquals("Username ya existe", exception.getMessage());
        verify(docenteRepository, never()).save(anyDocente());
    }

    @Test
    public void testRegistrarDocente_EmailFailureReturnsWarning() throws Exception {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(docenteRepository.existsByDni(anyLong())).thenReturn(false);
        when(docenteRepository.existsByCuil(anyLong())).thenReturn(false);

        Rol rol = new Rol();
        rol.setNombre("Docente");
        when(rolRepository.findByNombre("Docente")).thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Docente savedUser = new Docente();
        savedUser.setUsername("docente_test");
        when(docenteRepository.save(anyDocente())).thenReturn(savedUser);

        doThrow(new RuntimeException("SMTP Error")).when(emailService).sendWelcomeDocente(any(), anyString());

        StringBuilder warningMessage = new StringBuilder();
        Docente result = docenteService.registrarDocente(request, warningMessage);

        assertNotNull(result);
        assertTrue(warningMessage.toString().contains("falló el envío del correo"));
    }

    @Test
    public void testModificarPerfilDocente_SuccessGlobalAdmin() throws Exception {
        Usuario adminGlobal = new Usuario();
        adminGlobal.setUsername("admin_global");
        Rol rol = new Rol();
        rol.setNombre("ADMIN_GLOBAL");
        adminGlobal.setRol(rol);
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));

        Docente docente = new Docente();
        docente.setId(10);
        docente.setDni(12345678L);
        docente.setCuil(20123456780L);
        docente.setNombre("Carlos");
        docente.setApellido("Lopez");
        docente.setEmailInstitucional("carlos@facultad.edu");
        docente.setFechaIngreso(LocalDate.of(2018, 5, 10));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docente));

        DocenteProfileUpdateRequest updateReq = new DocenteProfileUpdateRequest();
        updateReq.setNombre("Carlos Alberto");
        updateReq.setApellido("Lopez Perez");
        updateReq.setEmailInstitucional("carlos.lopez@facultad.edu");
        updateReq.setFechaIngreso(LocalDate.of(2018, 5, 10));
        updateReq.setDni(12345678L);
        updateReq.setCuil(20123456780L);
        updateReq.setDomicilio("Calle Falsa 123");
        updateReq.setAntigPrevia(6);

        when(docenteRepository.existsByEmailInstitucionalAndIdNot(eq("carlos.lopez@facultad.edu"), eq(10))).thenReturn(false);
        when(usuarioRepository.existsByCorreoAndIdNot(eq("carlos.lopez@facultad.edu"), eq(10))).thenReturn(false);
        when(designacionRepository.findByUsuarioId(10)).thenReturn(java.util.List.of());

        DocenteDetailsDTO dto = docenteService.modificarPerfilDocente(10, updateReq, "admin_global");

        assertNotNull(dto);
        assertEquals("Carlos Alberto", docente.getNombre());
        assertEquals("Lopez Perez", docente.getApellido());
        assertEquals("carlos.lopez@facultad.edu", docente.getEmailInstitucional());
        verify(cambioService, atLeastOnce()).registrarCambio(eq("docentes"), anyString(), any(), any(), eq(adminGlobal));
        verify(docenteRepository, times(1)).save(docente);
    }

    @Test
    public void testModificarPerfilDocente_DniChangeThrowsBadRequest() {
        Usuario adminGlobal = new Usuario();
        adminGlobal.setUsername("admin_global");
        Rol rol = new Rol();
        rol.setNombre("ADMIN_GLOBAL");
        adminGlobal.setRol(rol);
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));

        Docente docente = new Docente();
        docente.setId(10);
        docente.setDni(12345678L);
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docente));

        DocenteProfileUpdateRequest updateReq = new DocenteProfileUpdateRequest();
        updateReq.setDni(99999999L); // Attempt to change immutable DNI
        updateReq.setNombre("Carlos");
        updateReq.setApellido("Lopez");
        updateReq.setEmailInstitucional("carlos@facultad.edu");
        updateReq.setFechaIngreso(LocalDate.of(2018, 5, 10));

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> docenteService.modificarPerfilDocente(10, updateReq, "admin_global")
        );

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("DNI es inmutable"));
    }

    @Test
    public void testModificarPerfilDocente_DuplicateEmailThrowsBadRequest() {
        Usuario adminGlobal = new Usuario();
        adminGlobal.setUsername("admin_global");
        Rol rol = new Rol();
        rol.setNombre("ADMIN_GLOBAL");
        adminGlobal.setRol(rol);
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));

        Docente docente = new Docente();
        docente.setId(10);
        docente.setDni(12345678L);
        docente.setEmailInstitucional("original@facultad.edu");
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docente));

        DocenteProfileUpdateRequest updateReq = new DocenteProfileUpdateRequest();
        updateReq.setDni(12345678L);
        updateReq.setNombre("Carlos");
        updateReq.setApellido("Lopez");
        updateReq.setEmailInstitucional("otro@facultad.edu");
        updateReq.setFechaIngreso(LocalDate.of(2018, 5, 10));

        when(docenteRepository.existsByEmailInstitucionalAndIdNot("otro@facultad.edu", 10)).thenReturn(true);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> docenteService.modificarPerfilDocente(10, updateReq, "admin_global")
        );

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("correo institucional ya se encuentra registrado"));
    }

    @Test
    public void testModificarPerfilDocente_UnitAdminOtherUnitThrowsForbidden() {
        Usuario adminUnidad = new Usuario();
        adminUnidad.setId(5);
        adminUnidad.setUsername("admin_unidad_1");
        Rol rol = new Rol();
        rol.setNombre("ADMIN_UNIDAD");
        adminUnidad.setRol(rol);
        when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(adminUnidad));

        AdminUnidad adminUnidadEntity = new AdminUnidad();
        com.atlas.atlas_backend.unidad.Unidad unidad1 = new com.atlas.atlas_backend.unidad.Unidad();
        unidad1.setUnidadId(1);
        adminUnidadEntity.setUnidad(unidad1);
        when(adminUnidadRepository.findById(5)).thenReturn(Optional.of(adminUnidadEntity));

        Docente docente = new Docente();
        docente.setId(20);
        when(docenteRepository.findById(20)).thenReturn(Optional.of(docente));

        // Teacher has no cargos in Unit 1
        when(designacionRepository.findByUsuarioId(20)).thenReturn(java.util.List.of());

        DocenteProfileUpdateRequest updateReq = new DocenteProfileUpdateRequest();
        updateReq.setNombre("Docente");
        updateReq.setApellido("Ajeno");

        org.springframework.security.access.AccessDeniedException ex = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> docenteService.modificarPerfilDocente(20, updateReq, "admin_unidad_1")
        );

        assertTrue(ex.getMessage().contains("No tiene permisos"));
    }
}


package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DesignacionServiceTest {

    @Mock
    private DesignacionRepository designacionRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DesignacionService designacionService;

    @NonNull
    private static Designacion anyDesignacion() {
        Designacion d = any(Designacion.class);
        return d != null ? d : new Designacion();
    }

    @NonNull
    private static Cargo anyCargo() {
        Cargo c = any(Cargo.class);
        return c != null ? c : new Cargo();
    }

    private Usuario adminGlobalUser;
    private Usuario adminUnidadUser;
    private AdminUnidad adminUnidad;
    private Unidad testUnidad;
    private Docente testDocente;
    private Cargo testCargo;
    private DesignacionCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        Rol rolAdminGlobal = new Rol();
        rolAdminGlobal.setNombre("ADMIN_GLOBAL");

        adminGlobalUser = new Usuario();
        adminGlobalUser.setId(1);
        adminGlobalUser.setUsername("admin_global");
        adminGlobalUser.setRol(rolAdminGlobal);

        Rol rolAdminUnidad = new Rol();
        rolAdminUnidad.setNombre("ADMIN_UNIDAD");

        adminUnidadUser = new Usuario();
        adminUnidadUser.setId(2);
        adminUnidadUser.setUsername("admin_unidad");
        adminUnidadUser.setRol(rolAdminUnidad);

        testUnidad = new Unidad();
        testUnidad.setUnidadId(1);
        testUnidad.setUnidadNombre("Facultad de Ingeniería");

        adminUnidad = new AdminUnidad();
        adminUnidad.setId(2);
        adminUnidad.setUnidad(testUnidad);

        testDocente = new Docente();
        testDocente.setId(10);
        testDocente.setNombre("Juan");
        testDocente.setApellido("Pérez");
        testDocente.setDni(30123456L);
        testDocente.setEmailInstitucional("juan.perez@atlas.edu");
        testDocente.setCorreo("juan.perez@atlas.edu");
        testDocente.setEstado("Inactivo");
        testDocente.setActivo(false);

        Categoria categoria = new Categoria();
        categoria.setCategoriaId(1);
        categoria.setCategoriaNombre("Profesor Titular");

        Dedicacion dedicacion = new Dedicacion();
        dedicacion.setDedicacionId(1);
        dedicacion.setDedicacionNombre("Exclusiva");

        testCargo = new Cargo();
        testCargo.setId(25);
        testCargo.setCodigo(5001L);
        testCargo.setEstado("Libre");
        testCargo.setCategoria(categoria);
        testCargo.setDedicacion(dedicacion);
        testCargo.setUnidad(testUnidad);

        validRequest = new DesignacionCreateRequest();
        validRequest.setDocenteId(10);
        validRequest.setCargoId(25);
        validRequest.setNroResolucion("1024");
        validRequest.setFechaInicio(LocalDate.of(2026, 3, 1));
        validRequest.setFechaFin(LocalDate.of(2027, 2, 28));
    }

    @Test
    void testT1_RegistrarDesignacion_AdminGlobal_Success() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(100);
            return d;
        });

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_global");

        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals("1024", response.getNroResolucion());
        assertEquals(LocalDate.of(2026, 3, 1), response.getFechaInicio());
        assertEquals(LocalDate.of(2027, 2, 28), response.getFechaFin());
        assertEquals("Activo", response.getEstado());

        // Verify Docente transitioned to Activo
        assertEquals("Activo", testDocente.getEstado());
        assertTrue(testDocente.getActivo());
        verify(docenteRepository, times(1)).save(Objects.requireNonNull(testDocente));

        // Verify Cargo transitioned to Asignado and linked to Designacion
        assertEquals("Asignado", testCargo.getEstado());
        assertNotNull(testCargo.getDesignacion());
        assertEquals(100, testCargo.getDesignacion().getDesignacionId());
        verify(cargoRepository, times(1)).save(Objects.requireNonNull(testCargo));

        // Verify DTO nested summaries
        assertNotNull(response.getDocente());
        assertEquals(10, response.getDocente().getId());
        assertEquals("Juan", response.getDocente().getNombre());
        assertEquals("Pérez", response.getDocente().getApellido());
        assertEquals(30123456L, response.getDocente().getDni());
        assertEquals("juan.perez@atlas.edu", response.getDocente().getEmail());

        assertNotNull(response.getCargo());
        assertEquals(25, response.getCargo().getId());
        assertEquals(5001L, response.getCargo().getCodigo());
        assertEquals("Profesor Titular", response.getCargo().getCategoria());
        assertEquals("Exclusiva", response.getCargo().getDedicacion());
        assertEquals("Facultad de Ingeniería", response.getCargo().getUnidad());

        // Verify email notification called
        verify(emailService, times(1)).sendDesignacionNotification(
                "juan.perez@atlas.edu",
                "1024",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 2, 28));
    }

    @Test
    void testT2_RegistrarDesignacion_OpenEndedFechaFinNull_Success() {
        validRequest.setFechaFin(null);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(101);
            return d;
        });

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_global");

        assertNotNull(response);
        assertEquals(101, response.getId());
        assertNull(response.getFechaFin());
        assertEquals("Activo", response.getEstado());
        assertEquals("Activo", testDocente.getEstado());
        assertEquals("Asignado", testCargo.getEstado());
    }

    @Test
    void testT3_RegistrarDesignacion_AdminUnidad_OwnUnit_Success() {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(102);
            return d;
        });

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_unidad");

        assertNotNull(response);
        assertEquals(102, response.getId());
        assertEquals("Activo", response.getEstado());
        assertEquals("Asignado", testCargo.getEstado());
    }

    @Test
    void testT4_RegistrarDesignacion_AdminUnidad_OtherUnit_ThrowsAccessDenied() {
        Unidad otraUnidad = new Unidad();
        otraUnidad.setUnidadId(2);
        otraUnidad.setUnidadNombre("Facultad de Ciencias Exactas");
        testCargo.setUnidad(otraUnidad);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_unidad"));

        assertEquals("No tiene permisos para registrar designaciones en cargos de otra unidad", ex.getMessage());
        verify(designacionRepository, never()).save(anyDesignacion());
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void testT5_RegistrarDesignacion_DocenteNotFound_Throws404() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Docente no encontrado", ex.getReason());
        verify(designacionRepository, never()).save(anyDesignacion());
    }

    @Test
    void testT6_RegistrarDesignacion_CargoNotFound_Throws404() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Cargo no encontrado", ex.getReason());
        verify(designacionRepository, never()).save(anyDesignacion());
    }

    @Test
    void testT7_RegistrarDesignacion_CargoNotLibre_Throws400() {
        testCargo.setEstado("Asignado");

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El cargo ya se encuentra asignado o en licencia", ex.getReason());
        verify(designacionRepository, never()).save(anyDesignacion());
    }

    @Test
    void testT8_RegistrarDesignacion_DuplicateResolucionForSameDocente_Throws400() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El docente ya posee una designación registrada con el número de resolución indicado", ex.getReason());
        verify(designacionRepository, never()).save(anyDesignacion());
    }

    @Test
    void testT9_RegistrarDesignacion_InvalidDates_FechaFinBeforeFechaInicio_Throws400() {
        validRequest.setFechaInicio(LocalDate.of(2026, 10, 1));
        validRequest.setFechaFin(LocalDate.of(2026, 5, 1));

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.registrarDesignacion(validRequest, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("La fecha de finalización no puede ser anterior a la fecha de inicio", ex.getReason());
        verify(designacionRepository, never()).save(anyDesignacion());
    }

    @Test
    void testT11_RegistrarDesignacion_EmailFailure_IsFailSafeAndSucceeds() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(105);
            return d;
        });
        doThrow(new RuntimeException("SMTP Server Down"))
                .when(emailService).sendDesignacionNotification(any(), any(), any(), any());

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_global");

        assertNotNull(response);
        assertEquals(105, response.getId());
        assertEquals("Activo", response.getEstado());
        assertEquals("Activo", testDocente.getEstado());
        assertEquals("Asignado", testCargo.getEstado());
    }

    @Test
    void testObtenerPorId_Success() {
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);
        designacion.setDesignacionNumeroResolucion("1024");
        designacion.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        designacion.setDesignacionFechaFin(LocalDate.of(2027, 2, 28));
        designacion.setDesignacionEstadoActual("Activo");
        designacion.setUsuarioId(10);

        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of(testCargo));

        DesignacionResponseDTO dto = designacionService.obtenerPorId(100);

        assertNotNull(dto);
        assertEquals(100, dto.getId());
        assertEquals("1024", dto.getNroResolucion());
        assertNotNull(dto.getDocente());
        assertEquals("Juan", dto.getDocente().getNombre());
        assertNotNull(dto.getCargo());
        assertEquals(5001L, dto.getCargo().getCodigo());
    }
}

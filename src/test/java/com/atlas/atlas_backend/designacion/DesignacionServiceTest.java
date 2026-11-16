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

    @NonNull
    private static Docente anyDocente() {
        Docente d = any(Docente.class);
        return d != null ? d : new Docente();
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

        // Verify email notification is deferred to RF-11 (Decisión R1)
        verify(emailService, never()).sendDesignacionNotification(any(), any(), any(), any());
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
        assertEquals("El docente ya posee una designación registrada con el número de resolución indicado",
                ex.getReason());
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
    void testT11_RegistrarDesignacion_DoesNotSendEmail_DeferredToRF11() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(105);
            return d;
        });

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_global");

        assertNotNull(response);
        assertEquals(105, response.getId());
        assertEquals("Activo", response.getEstado());
        assertEquals("Activo", testDocente.getEstado());
        assertEquals("Asignado", testCargo.getEstado());
        verify(emailService, never()).sendDesignacionNotification(any(), any(), any(), any());
    }

    @Test
    void testVincularCargo_T1_AdminGlobal_Success() {
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);
        designacion.setUsuarioId(10);
        designacion.setDesignacionNumeroResolucion("1024");
        designacion.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        designacion.setDesignacionFechaFin(LocalDate.of(2027, 2, 28));
        designacion.setDesignacionEstadoActual("Activo");

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of());
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));

        DesignacionResponseDTO response = designacionService.vincularCargo(100, 25, "admin_global");

        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals("1024", response.getNroResolucion());
        assertEquals("Asignado", testCargo.getEstado());
        assertEquals(designacion, testCargo.getDesignacion());
        verify(cargoRepository, times(1)).save(Objects.requireNonNull(testCargo));
        verify(emailService, never()).sendDesignacionNotification(any(), any(), any(), any());

        assertNotNull(response.getDocente());
        assertEquals(10, response.getDocente().getId());
        assertNotNull(response.getCargo());
        assertEquals(25, response.getCargo().getId());
        assertEquals(5001L, response.getCargo().getCodigo());
        assertEquals("Profesor Titular", response.getCargo().getCategoria());
    }

    @Test
    void testVincularCargo_T2_AdminUnidad_OwnUnit_Success() {
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);
        designacion.setUsuarioId(10);
        designacion.setDesignacionNumeroResolucion("1024");

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of());
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));

        DesignacionResponseDTO response = designacionService.vincularCargo(100, 25, "admin_unidad");

        assertNotNull(response);
        assertEquals("Asignado", testCargo.getEstado());
        assertEquals(designacion, testCargo.getDesignacion());
        verify(cargoRepository, times(1)).save(Objects.requireNonNull(testCargo));
    }

    @Test
    void testVincularCargo_T3_AdminUnidad_OtherUnit_ThrowsAccessDenied() {
        Unidad otraUnidad = new Unidad();
        otraUnidad.setUnidadId(2);
        otraUnidad.setUnidadNombre("Facultad de Ciencias Exactas");
        testCargo.setUnidad(otraUnidad);

        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of());
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> designacionService.vincularCargo(100, 25, "admin_unidad"));

        assertEquals("No tiene permisos para vincular cargos de otra unidad", ex.getMessage());
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void testVincularCargo_T4_CargoNotLibre_Throws400() {
        testCargo.setEstado("Asignado");
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of());
        when(cargoRepository.findById(25)).thenReturn(Optional.of(testCargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.vincularCargo(100, 25, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El cargo ya se encuentra asignado o en licencia", ex.getReason());
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void testVincularCargo_T5_CargoNotFound_Throws404() {
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of());
        when(cargoRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.vincularCargo(100, 999, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Cargo no encontrado", ex.getReason());
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void testVincularCargo_T6_DesignacionNotFound_Throws404() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.vincularCargo(999, 25, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Designación no encontrada", ex.getReason());
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void testVincularCargo_T7_DesignacionAlreadyHasCargo_Throws400() {
        Designacion designacion = new Designacion();
        designacion.setDesignacionId(100);

        Cargo alreadyLinkedCargo = new Cargo();
        alreadyLinkedCargo.setId(50);
        alreadyLinkedCargo.setDesignacion(designacion);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findById(100)).thenReturn(Optional.of(designacion));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of(alreadyLinkedCargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> designacionService.vincularCargo(100, 25, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("La designación ya posee un cargo vinculado", ex.getReason());
        verify(cargoRepository, never()).save(anyCargo());
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

    @Test
    void testRegistrarDesignacion_WithoutCargo_DoesNotActivateDocenteOrLinkCargo() {
        validRequest.setCargoId(null);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(testDocente));
        when(designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(10, "1024")).thenReturn(false);
        when(designacionRepository.save(anyDesignacion())).thenAnswer(inv -> {
            Designacion d = inv.getArgument(0);
            d.setDesignacionId(110);
            return d;
        });

        DesignacionResponseDTO response = designacionService.registrarDesignacion(validRequest, "admin_global");

        assertNotNull(response);
        assertEquals(110, response.getId());
        assertEquals("1024", response.getNroResolucion());
        assertNull(response.getCargo());

        // Docente remains Inactivo until subsequent confirmation steps
        assertEquals("Inactivo", testDocente.getEstado());
        verify(docenteRepository, never()).save(anyDocente());
        verify(cargoRepository, never()).save(anyCargo());
    }

    // =========================================================================
    // RF-36: Próximos Vencimientos y Resumen (spec-01 & spec-02)
    // =========================================================================

    @Test
    void testRF36_Spec01_Scenario1_AdminGlobal_ProximosVencimientos30Dias_OrderedAsc() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Designacion d1 = new Designacion();
        d1.setDesignacionId(1);
        d1.setDesignacionNumeroResolucion("RES-GLOBAL-01");
        d1.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        d1.setDesignacionFechaFin(LocalDate.of(2026, 11, 26)); // +10 días
        d1.setDesignacionEstadoActual("Activa");
        d1.setUsuarioId(10);

        Designacion d2 = new Designacion();
        d2.setDesignacionId(2);
        d2.setDesignacionNumeroResolucion("RES-GLOBAL-02");
        d2.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        d2.setDesignacionFechaFin(LocalDate.of(2026, 12, 1)); // +15 días
        d2.setDesignacionEstadoActual("Activa");
        d2.setUsuarioId(10);

        Designacion d3 = new Designacion();
        d3.setDesignacionId(3);
        d3.setDesignacionNumeroResolucion("RES-GLOBAL-03");
        d3.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        d3.setDesignacionFechaFin(LocalDate.of(2026, 12, 25)); // +39 días (fuera de umbral 30 días)
        d3.setDesignacionEstadoActual("Activa");
        d3.setUsuarioId(10);

        Cargo c1 = new Cargo();
        c1.setId(88);
        c1.setCodigo(100234L);
        c1.setDesignacion(d1);
        c1.setUnidad(testUnidad);
        c1.setCategoria(testCargo.getCategoria());
        c1.setDedicacion(testCargo.getDedicacion());

        Unidad unidad2 = new Unidad();
        unidad2.setUnidadId(2);
        unidad2.setUnidadNombre("Facultad de Ciencias Exactas");

        Cargo c2 = new Cargo();
        c2.setId(89);
        c2.setCodigo(100235L);
        c2.setDesignacion(d2);
        c2.setUnidad(unidad2);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findProximosVencimientos(null, fechaRef, fechaRef.plusDays(30)))
                .thenReturn(List.of(d2, d1, d3));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(2, 1, 3)))
                .thenReturn(List.of(c1, c2));
        when(docenteRepository.findAllById(Objects.requireNonNull(List.of(10)))).thenReturn(List.of(testDocente));

        List<DesignacionVencimientoDTO> result = designacionService.obtenerVencimientos(
                null, false, "admin_global", fechaRef);

        assertEquals(2, result.size());
        assertEquals("RES-GLOBAL-01", result.get(0).getNumeroResolucion());
        assertEquals(10L, result.get(0).getDiasRestantes());
        assertEquals("RES-GLOBAL-02", result.get(1).getNumeroResolucion());
        assertEquals(15L, result.get(1).getDiasRestantes());
        assertNotNull(result.get(0).getDocente());
        assertEquals("Juan", result.get(0).getDocente().getNombre());
        assertEquals("30123456", result.get(0).getDocente().getDocumento());
        assertNotNull(result.get(0).getUnidad());
        assertEquals("Facultad de Ingeniería", result.get(0).getUnidad().getNombre());
        assertEquals(1, result.get(0).getCargos().size());
        assertEquals(100234L, result.get(0).getCargos().get(0).getCodigo());
    }

    @Test
    void testRF36_Spec01_Scenario2_AdminUnidad_SoloSuPropiaUnidad() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Unidad unidadIng = new Unidad();
        unidadIng.setUnidadId(2);
        unidadIng.setUnidadNombre("Facultad de Ingeniería");
        adminUnidad.setUnidad(unidadIng);

        Designacion dIng = new Designacion();
        dIng.setDesignacionId(10);
        dIng.setDesignacionNumeroResolucion("RES-ING-01");
        dIng.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        dIng.setDesignacionFechaFin(LocalDate.of(2026, 11, 30));
        dIng.setDesignacionEstadoActual("Activa");
        dIng.setUsuarioId(10);

        Cargo cargoIng = new Cargo();
        cargoIng.setId(50);
        cargoIng.setCodigo(2001L);
        cargoIng.setDesignacion(dIng);
        cargoIng.setUnidad(unidadIng);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(designacionRepository.findProximosVencimientos(2, fechaRef, fechaRef.plusDays(30)))
                .thenReturn(List.of(dIng));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(10)))
                .thenReturn(List.of(cargoIng));
        when(docenteRepository.findAllById(Objects.requireNonNull(List.of(10)))).thenReturn(List.of(testDocente));

        List<DesignacionVencimientoDTO> result = designacionService.obtenerVencimientos(
                null, false, "admin_unidad", fechaRef);

        assertEquals(1, result.size());
        assertEquals("RES-ING-01", result.get(0).getNumeroResolucion());
        assertEquals(14L, result.get(0).getDiasRestantes());
        assertEquals(2, result.get(0).getUnidad().getUnidadId());
    }

    @Test
    void testRF36_Spec01_Scenario3_ExcluyeSinFechaFinOEstadoNoActivo() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Designacion dPerm = new Designacion();
        dPerm.setDesignacionId(1);
        dPerm.setDesignacionNumeroResolucion("RES-PERM-01");
        dPerm.setDesignacionFechaFin(null);
        dPerm.setDesignacionEstadoActual("Activa");

        Designacion dBaja = new Designacion();
        dBaja.setDesignacionId(2);
        dBaja.setDesignacionNumeroResolucion("RES-BAJA-01");
        dBaja.setDesignacionFechaFin(LocalDate.of(2026, 11, 20));
        dBaja.setDesignacionEstadoActual("Baja");

        Designacion dRev = new Designacion();
        dRev.setDesignacionId(3);
        dRev.setDesignacionNumeroResolucion("RES-REV-01");
        dRev.setDesignacionFechaFin(LocalDate.of(2026, 11, 22));
        dRev.setDesignacionEstadoActual("Revocada");

        Designacion dOk = new Designacion();
        dOk.setDesignacionId(4);
        dOk.setDesignacionNumeroResolucion("RES-OK-01");
        dOk.setDesignacionFechaFin(LocalDate.of(2026, 11, 25));
        dOk.setDesignacionEstadoActual("Activa");
        dOk.setUsuarioId(10);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findProximosVencimientos(null, fechaRef, fechaRef.plusDays(30)))
                .thenReturn(List.of(dPerm, dBaja, dRev, dOk));
        when(cargoRepository.findByDesignacionDesignacionIdIn(any())).thenReturn(List.of());
        when(docenteRepository.findAllById(Objects.requireNonNull(List.of(10)))).thenReturn(List.of(testDocente));

        List<DesignacionVencimientoDTO> result = designacionService.obtenerVencimientos(
                null, false, "admin_global", fechaRef);

        assertEquals(1, result.size());
        assertEquals("RES-OK-01", result.get(0).getNumeroResolucion());
    }

    @Test
    void testRF36_Spec01_Scenario4_IncluirVencidasTrue_RetornaDiasRestantesNegativos() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Designacion dVenc = new Designacion();
        dVenc.setDesignacionId(1);
        dVenc.setDesignacionNumeroResolucion("RES-VENC-01");
        dVenc.setDesignacionFechaFin(LocalDate.of(2026, 11, 10)); // -6 días
        dVenc.setDesignacionEstadoActual("Activa");
        dVenc.setUsuarioId(10);

        Designacion dProx = new Designacion();
        dProx.setDesignacionId(2);
        dProx.setDesignacionNumeroResolucion("RES-PROX-01");
        dProx.setDesignacionFechaFin(LocalDate.of(2026, 11, 20)); // +4 días
        dProx.setDesignacionEstadoActual("Activa");
        dProx.setUsuarioId(10);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.findVencimientosIncluyendoVencidas(null, fechaRef.plusDays(30)))
                .thenReturn(List.of(dVenc, dProx));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(1, 2))).thenReturn(List.of());
        when(docenteRepository.findAllById(Objects.requireNonNull(List.of(10)))).thenReturn(List.of(testDocente));

        List<DesignacionVencimientoDTO> result = designacionService.obtenerVencimientos(
                null, true, "admin_global", fechaRef);

        assertEquals(2, result.size());
        assertEquals("RES-VENC-01", result.get(0).getNumeroResolucion());
        assertEquals(-6L, result.get(0).getDiasRestantes());
        assertEquals("RES-PROX-01", result.get(1).getNumeroResolucion());
        assertEquals(4L, result.get(1).getDiasRestantes());
    }

    @Test
    void testRF36_Spec01_Scenario5_AdminUnidad_OtraUnidad_ThrowsAccessDenied() {
        Unidad unidad2 = new Unidad();
        unidad2.setUnidadId(2);
        unidad2.setUnidadNombre("Facultad de Ingeniería");
        adminUnidad.setUnidad(unidad2);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> designacionService.obtenerVencimientos(3, false, "admin_unidad"));

        assertEquals("No tiene permisos para consultar designaciones de otra unidad", ex.getMessage());
    }

    @Test
    void testRF36_Spec02_Scenario1_AdminGlobal_ResumenVencimientos() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(designacionRepository.countVencimientos(null, fechaRef, fechaRef.plusDays(30))).thenReturn(7L);
        when(designacionRepository.countVencimientos(null, fechaRef, fechaRef.plusDays(7))).thenReturn(2L);

        DesignacionVencimientosResumenDTO resumen = designacionService.obtenerResumenVencimientos(
                null, "admin_global", fechaRef);

        assertEquals(7L, resumen.getTotalPorVencer());
        assertEquals(30, resumen.getUmbralDias());
        assertNull(resumen.getUnidadId());
        assertNull(resumen.getUnidadNombre());
        assertTrue(resumen.getTieneVencimientosCriticos());
        assertEquals(2L, resumen.getTotalCriticos());
    }

    @Test
    void testRF36_Spec02_Scenario2_AdminUnidad_ResumenUnidad() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Unidad unidadExactas = new Unidad();
        unidadExactas.setUnidadId(4);
        unidadExactas.setUnidadNombre("Facultad de Ciencias Exactas");
        adminUnidad.setUnidad(unidadExactas);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(designacionRepository.countVencimientos(4, fechaRef, fechaRef.plusDays(30))).thenReturn(3L);
        when(designacionRepository.countVencimientos(4, fechaRef, fechaRef.plusDays(7))).thenReturn(1L);

        DesignacionVencimientosResumenDTO resumen = designacionService.obtenerResumenVencimientos(
                null, "admin_unidad", fechaRef);

        assertEquals(3L, resumen.getTotalPorVencer());
        assertEquals(4, resumen.getUnidadId());
        assertEquals("Facultad de Ciencias Exactas", resumen.getUnidadNombre());
        assertTrue(resumen.getTieneVencimientosCriticos());
        assertEquals(1L, resumen.getTotalCriticos());
    }

    @Test
    void testRF36_Spec02_Scenario3_SinVencimientos_RetornaCeroYCriticosFalse() {
        LocalDate fechaRef = LocalDate.of(2026, 11, 16);

        Unidad unidad5 = new Unidad();
        unidad5.setUnidadId(5);
        unidad5.setUnidadNombre("Facultad de Derecho");
        adminUnidad.setUnidad(unidad5);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(designacionRepository.countVencimientos(5, fechaRef, fechaRef.plusDays(30))).thenReturn(0L);
        when(designacionRepository.countVencimientos(5, fechaRef, fechaRef.plusDays(7))).thenReturn(0L);

        DesignacionVencimientosResumenDTO resumen = designacionService.obtenerResumenVencimientos(
                null, "admin_unidad", fechaRef);

        assertEquals(0L, resumen.getTotalPorVencer());
        assertFalse(resumen.getTieneVencimientosCriticos());
        assertEquals(0L, resumen.getTotalCriticos());
    }
}

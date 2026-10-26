package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.cambio.CambioService;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.designacion.DesignacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @NonNull
    private static Usuario anyUsuario() {
        Usuario u = any(Usuario.class);
        return u != null ? u : new Usuario();
    }

    @NonNull
    private static Docente anyDocente() {
        Docente d = any(Docente.class);
        return d != null ? d : new Docente();
    }

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private AdminGlobalRepository adminGlobalRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private AuditorRepository auditorRepository;

    @Mock
    private DesignacionRepository designacionRepository;

    @Mock
    private CambioService cambioService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario adminUser;
    private Usuario targetUser;
    private Rol rolAdminGlobal;
    private Rol rolAdminUnidad;
    private Rol rolDocente;

    @BeforeEach
    public void setUp() {
        rolAdminGlobal = new Rol();
        rolAdminGlobal.setId(1);
        rolAdminGlobal.setNombre("ADMIN_GLOBAL");

        rolAdminUnidad = new Rol();
        rolAdminUnidad.setId(2);
        rolAdminUnidad.setNombre("ADMIN_UNIDAD");

        rolDocente = new Rol();
        rolDocente.setId(4);
        rolDocente.setNombre("Docente");

        adminUser = new Usuario();
        adminUser.setId(1);
        adminUser.setUsername("superadmin");
        adminUser.setActivo(true);
        adminUser.setRol(rolAdminGlobal);

        targetUser = new Usuario();
        targetUser.setId(10);
        targetUser.setUsername("target.user");
        targetUser.setNombre("Juan");
        targetUser.setApellido("Perez");
        targetUser.setCorreo("juan.perez@test.com");
        targetUser.setActivo(true);
        targetUser.setRol(rolAdminUnidad);
    }

    @Test
    public void testTC01_DarDeBajaUsuario_NonDocente_Success() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(targetUser));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));
        when(docenteRepository.findById(10)).thenReturn(Optional.empty());
        when(adminGlobalRepository.findById(10)).thenReturn(Optional.empty());
        when(adminUnidadRepository.findById(10)).thenReturn(Optional.empty());
        when(auditorRepository.findById(10)).thenReturn(Optional.empty());

        Map<String, Object> response = usuarioService.darDeBajaUsuario(10, "superadmin");

        assertNotNull(response);
        assertEquals("Usuario dado de baja exitosamente", response.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, Object> usuarioMap = (Map<String, Object>) response.get("usuario");
        assertNotNull(usuarioMap);
        assertEquals(false, usuarioMap.get("activo"));
        assertEquals("target.user", usuarioMap.get("username"));

        assertFalse(targetUser.getActivo());
        verify(usuarioRepository).save(Objects.requireNonNull(targetUser));
        verify(cambioService).registrarCambio(eq("usuarios"), eq("usuario_activo"), eq("true"), eq("false"),
                eq(adminUser));
        verify(docenteRepository, never()).save(anyDocente());
    }

    @Test
    public void testTC02_DarDeBajaDocente_NoActiveDesignaciones_Success() {
        Docente docente = new Docente();
        docente.setId(20);
        docente.setUsername("docente.test");
        docente.setNombre("Maria");
        docente.setApellido("Gomez");
        docente.setCorreo("maria@test.com");
        docente.setActivo(true);
        docente.setRol(rolDocente);
        docente.setEstado("Activo");
        docente.setDni(12345678L);
        docente.setCuil(20123456783L);

        Designacion finalizada = new Designacion();
        finalizada.setDesignacionId(101);
        finalizada.setUsuarioId(20);
        finalizada.setDesignacionEstadoActual("Finalizada");
        finalizada.setDesignacionFechaFin(LocalDate.now().minusDays(10));

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(docente));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));
        when(docenteRepository.findById(20)).thenReturn(Optional.of(docente));
        when(designacionRepository.findByUsuarioId(20)).thenReturn(List.of(finalizada));

        Map<String, Object> response = usuarioService.darDeBajaUsuario(20, "superadmin");

        assertNotNull(response);
        assertEquals("Usuario dado de baja exitosamente", response.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, Object> usuarioMap = (Map<String, Object>) response.get("usuario");
        assertEquals(false, usuarioMap.get("activo"));
        assertEquals("Inactivo", usuarioMap.get("estado"));

        assertFalse(docente.getActivo());
        assertEquals("Inactivo", docente.getEstado());

        verify(usuarioRepository).save(Objects.requireNonNull(docente));
        verify(docenteRepository).save(Objects.requireNonNull(docente));
        verify(cambioService).registrarCambio(eq("usuarios"), eq("usuario_activo"), eq("true"), eq("false"),
                eq(adminUser));
        verify(cambioService).registrarCambio(eq("docentes"), eq("docente_estado"), eq("Activo"), eq("Inactivo"),
                eq(adminUser));
    }

    @Test
    public void testTC03_DarDeBajaDocente_WithActiveDesignacion_ThrowsConflict() {
        Docente docente = new Docente();
        docente.setId(25);
        docente.setUsername("docente.activo");
        docente.setActivo(true);
        docente.setRol(rolDocente);
        docente.setEstado("Activo");

        Designacion activa = new Designacion();
        activa.setDesignacionId(102);
        activa.setUsuarioId(25);
        activa.setDesignacionEstadoActual("Vigente");
        activa.setDesignacionFechaFin(LocalDate.now().plusMonths(3));

        when(usuarioRepository.findById(25)).thenReturn(Optional.of(docente));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));
        when(docenteRepository.findById(25)).thenReturn(Optional.of(docente));
        when(designacionRepository.findByUsuarioId(25)).thenReturn(List.of(activa));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.darDeBajaUsuario(25, "superadmin"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("posee cargos vigentes"));
        verify(usuarioRepository, never()).save(anyUsuario());
        verify(cambioService, never()).registrarCambio(any(), any(), any(), any(), any());
    }

    @Test
    public void testTC04_DarDeBajaUsuario_NotFound_Throws404() {
        when(usuarioRepository.findById(99999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.darDeBajaUsuario(99999, "superadmin"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("Usuario no encontrado"));
    }

    @Test
    public void testTC05_DarDeBajaUsuario_AlreadyInactive_ThrowsBadRequest() {
        targetUser.setActivo(false);

        when(usuarioRepository.findById(30)).thenReturn(Optional.of(targetUser));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.darDeBajaUsuario(30, "superadmin"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("ya se encuentra inactivo"));
        verify(usuarioRepository, never()).save(anyUsuario());
    }

    @Test
    public void testTC06_DarDeBajaUsuario_SelfDecommission_ThrowsBadRequest() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.darDeBajaUsuario(1, "superadmin"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("No puede darse de baja a sí mismo"));
        verify(usuarioRepository, never()).save(anyUsuario());
    }

    @Test
    public void testTC07_DarDeBajaDocente_DesignacionWithoutEndDate_TreatedAsActive() {
        Docente docente = new Docente();
        docente.setId(26);
        docente.setUsername("docente.sinfin");
        docente.setActivo(true);
        docente.setRol(rolDocente);
        docente.setEstado("Activo");

        Designacion activaIndefinida = new Designacion();
        activaIndefinida.setDesignacionId(103);
        activaIndefinida.setUsuarioId(26);
        activaIndefinida.setDesignacionEstadoActual("Regular");
        activaIndefinida.setDesignacionFechaFin(null); // No end date

        when(usuarioRepository.findById(26)).thenReturn(Optional.of(docente));
        when(usuarioRepository.findByUsername("superadmin")).thenReturn(Optional.of(adminUser));
        when(docenteRepository.findById(26)).thenReturn(Optional.of(docente));
        when(designacionRepository.findByUsuarioId(26)).thenReturn(List.of(activaIndefinida));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.darDeBajaUsuario(26, "superadmin"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("posee cargos vigentes"));
    }
}

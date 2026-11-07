package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.catedra.dto.CatedraRequestDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraResponseDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraUpdateRequestDTO;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.materia.MateriaRepository;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CatedraServiceTest {

    @Mock
    private CatedraRepository catedraRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @InjectMocks
    private CatedraService catedraService;

    @org.springframework.lang.NonNull
    private static Catedra anyCatedra() {
        Catedra c = any(Catedra.class);
        return c != null ? c : new Catedra();
    }

    private Unidad unidad5;
    private Unidad unidad9;
    private Materia materia1;
    private Materia materia2;
    private Docente docenteActivo;
    private Docente docenteActivo2;
    private Docente docenteInactivo;
    private Usuario adminGlobal;
    private Usuario adminUnidadUser;
    private AdminUnidad adminUnidadRecord;
    private CatedraRequestDTO validRequest;
    private CatedraUpdateRequestDTO validUpdateRequest;
    private Catedra catedraActiva;
    private Catedra catedraDeBaja;

    @BeforeEach
    void setUp() {
        unidad5 = new Unidad();
        unidad5.setUnidadId(5);
        unidad5.setUnidadNombre("Facultad de Ingeniería");

        unidad9 = new Unidad();
        unidad9.setUnidadId(9);
        unidad9.setUnidadNombre("Facultad de Medicina");

        materia1 = new Materia();
        materia1.setId(1);
        materia1.setNombre("Programación I");
        materia1.setUnidad(unidad5);

        materia2 = new Materia();
        materia2.setId(2);
        materia2.setNombre("Anatomía General");
        materia2.setUnidad(unidad9);

        docenteActivo = new Docente();
        docenteActivo.setId(10);
        docenteActivo.setNombre("Carlos");
        docenteActivo.setApellido("Pérez");
        docenteActivo.setEstado("Activo");

        docenteActivo2 = new Docente();
        docenteActivo2.setId(15);
        docenteActivo2.setNombre("Laura");
        docenteActivo2.setApellido("Martínez");
        docenteActivo2.setEstado("Activo");

        docenteInactivo = new Docente();
        docenteInactivo.setId(12);
        docenteInactivo.setNombre("Ana");
        docenteInactivo.setApellido("Gómez");
        docenteInactivo.setEstado("De Baja");

        Rol rolGlobal = new Rol();
        rolGlobal.setNombre("ADMIN_GLOBAL");
        adminGlobal = new Usuario();
        adminGlobal.setId(1);
        adminGlobal.setUsername("admin.global");
        adminGlobal.setRol(rolGlobal);

        Rol rolUnidad = new Rol();
        rolUnidad.setNombre("ADMIN_UNIDAD");
        adminUnidadUser = new Usuario();
        adminUnidadUser.setId(2);
        adminUnidadUser.setUsername("admin.unidad");
        adminUnidadUser.setRol(rolUnidad);

        adminUnidadRecord = new AdminUnidad();
        adminUnidadRecord.setId(2);
        adminUnidadRecord.setUnidad(unidad5);

        validRequest = CatedraRequestDTO.builder()
                .nombre("Cátedra A")
                .tipologia(1)
                .directorId(10)
                .materiaId(1)
                .build();

        validUpdateRequest = CatedraUpdateRequestDTO.builder()
                .nombre("Cátedra A Renovada")
                .tipologia(2)
                .directorId(15)
                .build();

        catedraActiva = new Catedra();
        catedraActiva.setId(10);
        catedraActiva.setNombre("Cátedra A");
        catedraActiva.setTipologia(1);
        catedraActiva.setEstado("Activa");
        catedraActiva.setDirector(docenteActivo);
        catedraActiva.setMateria(materia1);

        catedraDeBaja = new Catedra();
        catedraDeBaja.setId(11);
        catedraDeBaja.setNombre("Cátedra Antigua");
        catedraDeBaja.setTipologia(1);
        catedraDeBaja.setEstado("De Baja");
        catedraDeBaja.setDirector(docenteActivo);
        catedraDeBaja.setMateria(materia1);
    }

    @Test
    @DisplayName("Scenario 1: Admin Global crea cátedra válida exitosamente")
    void crearCatedra_AdminGlobal_Exito() {
        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docenteActivo));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_Id("Cátedra A", 1)).thenReturn(false);

        Catedra saved = new Catedra();
        saved.setId(100);
        saved.setNombre("Cátedra A");
        saved.setTipologia(1);
        saved.setEstado("Activa");
        saved.setDirector(docenteActivo);
        saved.setMateria(materia1);

        when(catedraRepository.save(anyCatedra())).thenReturn(saved);

        CatedraResponseDTO response = catedraService.crearCatedra(validRequest, "admin.global");

        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals("Cátedra A", response.getNombre());
        assertEquals(1, response.getTipologia());
        assertEquals("Activa", response.getEstado());
        assertEquals(10, response.getDirectorId());
        assertEquals("Carlos Pérez", response.getDirectorNombre());
        assertEquals(1, response.getMateriaId());
        assertEquals("Programación I", response.getMateriaNombre());

        verify(catedraRepository, times(1)).save(anyCatedra());
    }

    @Test
    @DisplayName("Scenario 2: Admin Unidad crea cátedra en materia de su propia unidad exitosamente")
    void crearCatedra_AdminUnidad_MismaUnidad_Exito() {
        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidadRecord));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docenteActivo));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_Id("Cátedra A", 1)).thenReturn(false);

        Catedra saved = new Catedra();
        saved.setId(101);
        saved.setNombre("Cátedra A");
        saved.setTipologia(1);
        saved.setEstado("Activa");
        saved.setDirector(docenteActivo);
        saved.setMateria(materia1);

        when(catedraRepository.save(anyCatedra())).thenReturn(saved);

        CatedraResponseDTO response = catedraService.crearCatedra(validRequest, "admin.unidad");

        assertNotNull(response);
        assertEquals(101, response.getId());
        assertEquals("Activa", response.getEstado());
    }

    @Test
    @DisplayName("Scenario 3: Rechazo por cátedra ya existente en la misma materia")
    void crearCatedra_NombreDuplicadoEnMismaMateria_LanzaBadRequest() {
        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docenteActivo));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_Id("Cátedra A", 1)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.crearCatedra(validRequest, "admin.global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Ya existe una cátedra con ese nombre"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("Scenario 4: Rechazo por director en estado inactivo o de baja")
    void crearCatedra_DirectorInactivo_LanzaBadRequest() {
        validRequest.setDirectorId(12);

        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(12)).thenReturn(Optional.of(docenteInactivo));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.crearCatedra(validRequest, "admin.global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("El docente debe estar en estado activo"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("Scenario 4b: Alta exitosa asignando docente que ya dirige otra cátedra")
    void crearCatedra_DirectorConOtraCatedra_PermiteCreacion() {
        // En este escenario, no hay restricción unique, por lo que procede con
        // normalidad
        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(docenteActivo));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_Id("Cátedra A", 1)).thenReturn(false);

        Catedra saved = new Catedra();
        saved.setId(102);
        saved.setNombre("Cátedra A");
        saved.setTipologia(1);
        saved.setEstado("Activa");
        saved.setDirector(docenteActivo);
        saved.setMateria(materia1);

        when(catedraRepository.save(anyCatedra())).thenReturn(saved);

        CatedraResponseDTO response = catedraService.crearCatedra(validRequest, "admin.global");

        assertNotNull(response);
        assertEquals(102, response.getId());
        verify(catedraRepository, times(1)).save(anyCatedra());
    }

    @Test
    @DisplayName("Scenario 5: Rechazo cuando Admin de Unidad intenta crear cátedra en materia de otra unidad")
    void crearCatedra_AdminUnidad_MateriaDeOtraUnidad_LanzaForbidden() {
        // Admin unidad pertenece a unidad 5, materia2 pertenece a unidad 9
        validRequest.setMateriaId(2);

        when(materiaRepository.findById(2)).thenReturn(Optional.of(materia2));
        when(usuarioRepository.findByUsername("admin.unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidadRecord));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> catedraService.crearCatedra(validRequest, "admin.unidad"));

        assertTrue(exception.getMessage().contains("No tiene permisos para crear cátedras en materias de otra unidad"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("Scenario 6a: Rechazo cuando materia no existe")
    void crearCatedra_MateriaNoExiste_LanzaNotFound() {
        when(materiaRepository.findById(999)).thenReturn(Optional.empty());
        validRequest.setMateriaId(999);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.crearCatedra(validRequest, "admin.global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Materia no encontrada"));
    }

    @Test
    @DisplayName("Scenario 6b: Rechazo cuando docente director no existe")
    void crearCatedra_DocenteNoExiste_LanzaNotFound() {
        when(materiaRepository.findById(1)).thenReturn(Optional.of(materia1));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(999)).thenReturn(Optional.empty());
        validRequest.setDirectorId(999);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.crearCatedra(validRequest, "admin.global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Docente no encontrado"));
    }

    // =========================================================================
    // RF-22: Modificar los datos de una cátedra
    // =========================================================================

    @Test
    @DisplayName("RF-22 Scenario 1: Admin Global modifica exitosamente nombre, tipología y director de cátedra activa")
    void modificarCatedra_AdminGlobal_Exito() {
        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(15)).thenReturn(Optional.of(docenteActivo2));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_IdAndIdNot("Cátedra A Renovada", 1, 10))
                .thenReturn(false);
        when(catedraRepository.save(anyCatedra())).thenAnswer(invocation -> invocation.getArgument(0));

        CatedraResponseDTO response = catedraService.modificarCatedra(10, validUpdateRequest, "admin.global");

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("Cátedra A Renovada", response.getNombre());
        assertEquals(2, response.getTipologia());
        assertEquals("Activa", response.getEstado());
        assertEquals(15, response.getDirectorId());
        assertEquals("Laura Martínez", response.getDirectorNombre());
        assertEquals(1, response.getMateriaId());
        assertEquals("Programación I", response.getMateriaNombre());

        verify(catedraRepository, times(1)).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 2: Modificación exitosa manteniendo el mismo nombre (exclusión de propio ID)")
    void modificarCatedra_MismoNombre_Exito() {
        validUpdateRequest.setNombre("Cátedra A");
        validUpdateRequest.setTipologia(3);

        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(15)).thenReturn(Optional.of(docenteActivo2));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_IdAndIdNot("Cátedra A", 1, 10)).thenReturn(false);
        when(catedraRepository.save(anyCatedra())).thenAnswer(invocation -> invocation.getArgument(0));

        CatedraResponseDTO response = catedraService.modificarCatedra(10, validUpdateRequest, "admin.global");

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("Cátedra A", response.getNombre());
        assertEquals(3, response.getTipologia());
        assertEquals(15, response.getDirectorId());
    }

    @Test
    @DisplayName("RF-22 Scenario 3: Admin Unidad modifica cátedra perteneciente a su propia unidad académica")
    void modificarCatedra_AdminUnidad_MismaUnidad_Exito() {
        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidadRecord));
        when(docenteRepository.findById(15)).thenReturn(Optional.of(docenteActivo2));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_IdAndIdNot("Cátedra A Renovada", 1, 10))
                .thenReturn(false);
        when(catedraRepository.save(anyCatedra())).thenAnswer(invocation -> invocation.getArgument(0));

        CatedraResponseDTO response = catedraService.modificarCatedra(10, validUpdateRequest, "admin.unidad");

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("Cátedra A Renovada", response.getNombre());
        assertEquals(2, response.getTipologia());
    }

    @Test
    @DisplayName("RF-22 Scenario 4: Rechazo cuando la cátedra a modificar no existe (404 Not Found)")
    void modificarCatedra_CatedraNoExiste_LanzaNotFound() {
        when(catedraRepository.findById(9999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.modificarCatedra(9999, validUpdateRequest, "admin.global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Cátedra no encontrada"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 5: Rechazo cuando la cátedra se encuentra dada de baja (400 Bad Request)")
    void modificarCatedra_CatedraDeBaja_LanzaBadRequest() {
        when(catedraRepository.findById(11)).thenReturn(Optional.of(catedraDeBaja));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.modificarCatedra(11, validUpdateRequest, "admin.global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("No se puede modificar una cátedra que no se encuentra en estado activa"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 6: Rechazo por colisión de nombre con otra cátedra de la misma materia (400 Bad Request)")
    void modificarCatedra_NombreDuplicadoEnMateria_LanzaBadRequest() {
        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(15)).thenReturn(Optional.of(docenteActivo2));
        when(catedraRepository.existsByNombreIgnoreCaseAndMateria_IdAndIdNot("Cátedra A Renovada", 1, 10))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.modificarCatedra(10, validUpdateRequest, "admin.global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Ya existe una cátedra con ese nombre para la materia seleccionada"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 7a: Rechazo por nuevo director en estado inactivo (400 Bad Request)")
    void modificarCatedra_DirectorInactivo_LanzaBadRequest() {
        validUpdateRequest.setDirectorId(12);

        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(12)).thenReturn(Optional.of(docenteInactivo));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.modificarCatedra(10, validUpdateRequest, "admin.global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("El docente debe estar en estado activo"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 7b: Rechazo por nuevo director inexistente (404 Not Found)")
    void modificarCatedra_DirectorNoExiste_LanzaNotFound() {
        validUpdateRequest.setDirectorId(999);

        when(catedraRepository.findById(10)).thenReturn(Optional.of(catedraActiva));
        when(usuarioRepository.findByUsername("admin.global")).thenReturn(Optional.of(adminGlobal));
        when(docenteRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catedraService.modificarCatedra(10, validUpdateRequest, "admin.global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        String reason = exception.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Docente no encontrado"));
        verify(catedraRepository, never()).save(anyCatedra());
    }

    @Test
    @DisplayName("RF-22 Scenario 8: Rechazo cuando Admin de Unidad intenta modificar cátedra de otra unidad (403 Forbidden)")
    void modificarCatedra_AdminUnidad_OtraUnidad_LanzaForbidden() {
        Catedra catedraOtraUnidad = new Catedra();
        catedraOtraUnidad.setId(30);
        catedraOtraUnidad.setNombre("Cátedra Anatomía");
        catedraOtraUnidad.setTipologia(1);
        catedraOtraUnidad.setEstado("Activa");
        catedraOtraUnidad.setDirector(docenteActivo);
        catedraOtraUnidad.setMateria(materia2); // unidad 9, adminUnidadRecord pertenece a unidad 5

        when(catedraRepository.findById(30)).thenReturn(Optional.of(catedraOtraUnidad));
        when(usuarioRepository.findByUsername("admin.unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidadRecord));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> catedraService.modificarCatedra(30, validUpdateRequest, "admin.unidad"));

        assertTrue(exception.getMessage()
                .contains("No tiene permisos para modificar cátedras en materias de otra unidad"));
        verify(catedraRepository, never()).save(anyCatedra());
    }
}

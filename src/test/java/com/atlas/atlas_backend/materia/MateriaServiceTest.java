package com.atlas.atlas_backend.materia;

import com.atlas.atlas_backend.unidad.TipoUnidad;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
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

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MateriaServiceTest {

        @Mock
        private MateriaRepository materiaRepository;

        @Mock
        private UnidadRepository unidadRepository;

        @Mock
        private UsuarioRepository usuarioRepository;

        @Mock
        private AdminUnidadRepository adminUnidadRepository;

        @org.springframework.lang.NonNull
        private static Materia anyMateria() {
                Materia m = any(Materia.class);
                return m != null ? m : new Materia();
        }

        @InjectMocks
        private MateriaService materiaService;

        private TipoUnidad tipoUnidad;
        private Unidad unidad1;
        private Unidad unidad2;
        private Usuario userGlobalAdmin;
        private Usuario userAdminUnidad;
        private AdminUnidad adminUnidadRecord;
        private MateriaCreateRequest validRequest;

        @BeforeEach
        void setUp() {
                tipoUnidad = new TipoUnidad();
                tipoUnidad.setId(10);
                tipoUnidad.setNombre("Departamento");

                unidad1 = new Unidad();
                unidad1.setUnidadId(1);
                unidad1.setUnidadNombre("Matemática");
                unidad1.setTipoUnidad(tipoUnidad);

                unidad2 = new Unidad();
                unidad2.setUnidadId(2);
                unidad2.setUnidadNombre("Computación");
                unidad2.setTipoUnidad(tipoUnidad);

                Rol rolGlobal = new Rol();
                rolGlobal.setId(1);
                rolGlobal.setNombre("ADMIN_GLOBAL");

                userGlobalAdmin = new Usuario();
                userGlobalAdmin.setId(100);
                userGlobalAdmin.setUsername("admin_global");
                userGlobalAdmin.setRol(rolGlobal);

                Rol rolUnidad = new Rol();
                rolUnidad.setId(2);
                rolUnidad.setNombre("ADMIN_UNIDAD");

                userAdminUnidad = new Usuario();
                userAdminUnidad.setId(200);
                userAdminUnidad.setUsername("admin_unidad_1");
                userAdminUnidad.setRol(rolUnidad);

                adminUnidadRecord = new AdminUnidad();
                adminUnidadRecord.setId(200);
                adminUnidadRecord.setUsername("admin_unidad_1");
                adminUnidadRecord.setUnidad(unidad1);

                validRequest = new MateriaCreateRequest();
                validRequest.setNombre("Álgebra Lineal");
                validRequest.setHoras(6);
                validRequest.setAnio("1°");
                validRequest.setDespliegue("Primer Semestre");
                validRequest.setUnidadId(1);
                validRequest.setDependenciaId(10);
                validRequest.setPlanEstudio(2023);
        }

        @Test
        @DisplayName("Scenario 1: Successful creation by ADMIN_GLOBAL (Happy Path)")
        void testCrearMateria_AdminGlobal_Success() {
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad1));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio("Álgebra Lineal", 1,
                                2023))
                                .thenReturn(false);

                Materia savedEntity = new Materia();
                savedEntity.setId(501);
                savedEntity.setNombre("Álgebra Lineal");
                savedEntity.setHoras(6);
                savedEntity.setAnio("1°");
                savedEntity.setDespliegue("Primer Semestre");
                savedEntity.setPlanEstudio(2023);
                savedEntity.setUnidad(unidad1);

                when(materiaRepository.save(anyMateria())).thenReturn(savedEntity);

                MateriaDetailDTO result = materiaService.crearMateria(validRequest, "admin_global");

                assertNotNull(result);
                assertEquals(501, result.getId());
                assertEquals("Álgebra Lineal", result.getNombre());
                assertEquals(6, result.getHoras());
                assertEquals("1°", result.getAnio());
                assertEquals("Primer Semestre", result.getDespliegue());
                assertEquals(2023, result.getPlanEstudio());
                assertEquals(1, result.getUnidadId());
                assertEquals("Matemática", result.getUnidadNombre());
                assertEquals(10, result.getDependenciaId());
                assertEquals("Departamento", result.getDependenciaNombre());

                verify(materiaRepository).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 2: Successful creation by ADMIN_UNIDAD within assigned unit")
        void testCrearMateria_AdminUnidad_Success() {
                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidadRecord));
                when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad1));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio("Álgebra Lineal", 1,
                                2023))
                                .thenReturn(false);

                Materia savedEntity = new Materia();
                savedEntity.setId(502);
                savedEntity.setNombre("Álgebra Lineal");
                savedEntity.setHoras(6);
                savedEntity.setAnio("1°");
                savedEntity.setDespliegue("Primer Semestre");
                savedEntity.setPlanEstudio(2023);
                savedEntity.setUnidad(unidad1);

                when(materiaRepository.save(anyMateria())).thenReturn(savedEntity);

                MateriaDetailDTO result = materiaService.crearMateria(validRequest, "admin_unidad_1");

                assertNotNull(result);
                assertEquals(502, result.getId());
                assertEquals("Álgebra Lineal", result.getNombre());
                verify(materiaRepository).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 3: Same subject name allowed in different study plans of same unit")
        void testCrearMateria_DifferentStudyPlan_Allowed() {
                validRequest.setPlanEstudio(2025);

                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad1));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio("Álgebra Lineal", 1,
                                2025))
                                .thenReturn(false);

                Materia savedEntity = new Materia();
                savedEntity.setId(503);
                savedEntity.setNombre("Álgebra Lineal");
                savedEntity.setHoras(6);
                savedEntity.setAnio("1°");
                savedEntity.setDespliegue("Primer Semestre");
                savedEntity.setPlanEstudio(2025);
                savedEntity.setUnidad(unidad1);

                when(materiaRepository.save(anyMateria())).thenReturn(savedEntity);

                MateriaDetailDTO result = materiaService.crearMateria(validRequest, "admin_global");

                assertNotNull(result);
                assertEquals(2025, result.getPlanEstudio());
        }

        @Test
        @DisplayName("Scenario 4: Rejection when ADMIN_UNIDAD registers for another unit (403 Forbidden)")
        void testCrearMateria_AdminUnidad_Forbidden_UnitMismatch() {
                validRequest.setUnidadId(2); // Attempting to register in Unidad 2 while assigned to Unidad 1

                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidadRecord));

                AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_unidad_1"));

                assertTrue(ex.getMessage().contains("No tiene permisos para registrar materias"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 5: Duplicate subject in same unit and study plan rejected (409 Conflict)")
        void testCrearMateria_DuplicateSubject_Conflict() {
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad1));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio("Álgebra Lineal", 1,
                                2023))
                                .thenReturn(true);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("Ya existe una materia"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 6: Out of range hours rejected (400 Bad Request)")
        void testCrearMateria_HoursOutOfRange_BadRequest() {
                validRequest.setHoras(0);
                ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));
                assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

                validRequest.setHoras(45);
                ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));
                assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());

                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 7: Invalid despliegue value rejected (400 Bad Request)")
        void testCrearMateria_InvalidDespliegue_BadRequest() {
                validRequest.setDespliegue("Trimestral");

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("El despliegue debe ser"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 8: Non-existent Unidad rejected (404 Not Found)")
        void testCrearMateria_UnidadNotFound() {
                validRequest.setUnidadId(9999);

                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(unidadRepository.findById(9999)).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));

                assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("Unidad académica no encontrada"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("Scenario 9: Dependencia mismatch with Unidad rejected (400 Bad Request)")
        void testCrearMateria_DependenciaMismatch_BadRequest() {
                validRequest.setDependenciaId(999); // Does not match tipoUnidad.id = 10

                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad1));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.crearMateria(validRequest, "admin_global"));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("La dependencia especificada no coincide"));
                verify(materiaRepository, never()).save(anyMateria());
        }
}

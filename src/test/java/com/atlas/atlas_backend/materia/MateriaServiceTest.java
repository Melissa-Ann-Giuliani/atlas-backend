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

        @Mock
        private com.atlas.atlas_backend.catedra.CatedraRepository catedraRepository;

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

        // ==================== Tests for modificarMateria ====================

        @Test
        @DisplayName("modificarMateria - Success: partial modification of name and year by ADMIN_GLOBAL")
        void testModificarMateria_AdminGlobal_Success() {
                Materia existing = new Materia();
                existing.setId(10);
                existing.setNombre("Álgebra I");
                existing.setHoras(4);
                existing.setAnio("1°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Álgebra Lineal");
                patchRequest.setAnio("2°");

                when(materiaRepository.findById(10)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot("Álgebra Lineal", 1, 2020, 10))
                                .thenReturn(false);
                when(materiaRepository.save(anyMateria())).thenAnswer(inv -> inv.getArgument(0));

                MateriaDetailDTO result = materiaService.modificarMateria(10, patchRequest, "admin_global");

                assertNotNull(result);
                assertEquals(10, result.getId());
                assertEquals("Álgebra Lineal", result.getNombre());
                assertEquals("2°", result.getAnio());
                assertEquals(4, result.getHoras()); // unchanged
                assertEquals("Primer Semestre", result.getDespliegue()); // unchanged
                assertEquals(2020, result.getPlanEstudio());
                assertEquals(1, result.getUnidadId());
                verify(materiaRepository).save(existing);
        }

        @Test
        @DisplayName("modificarMateria - Success: update hours and deployment when no active chairs exist")
        void testModificarMateria_HoursAndDespliegue_NoActiveCatedras_Success() {
                Materia existing = new Materia();
                existing.setId(15);
                existing.setNombre("Física I");
                existing.setHoras(4);
                existing.setAnio("1°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setHoras(6);
                patchRequest.setDespliegue("Anual");

                when(materiaRepository.findById(15)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(catedraRepository.existsByMateria_IdAndEstadoIgnoreCase(15, "ACTIVA")).thenReturn(false);
                when(materiaRepository.save(anyMateria())).thenAnswer(inv -> inv.getArgument(0));

                MateriaDetailDTO result = materiaService.modificarMateria(15, patchRequest, "admin_global");

                assertNotNull(result);
                assertEquals(6, result.getHoras());
                assertEquals("Anual", result.getDespliegue());
                verify(materiaRepository).save(existing);
        }

        @Test
        @DisplayName("modificarMateria - Conflict: update hours rejected when active chairs exist")
        void testModificarMateria_Hours_ActiveCatedras_Conflict() {
                Materia existing = new Materia();
                existing.setId(10);
                existing.setNombre("Álgebra I");
                existing.setHoras(4);
                existing.setAnio("1°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setHoras(8);

                when(materiaRepository.findById(10)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(catedraRepository.existsByMateria_IdAndEstadoIgnoreCase(10, "ACTIVA")).thenReturn(true);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.modificarMateria(10, patchRequest, "admin_global"));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("cátedras activas asociadas"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("modificarMateria - Conflict: update despliegue rejected when active chairs exist")
        void testModificarMateria_Despliegue_ActiveCatedras_Conflict() {
                Materia existing = new Materia();
                existing.setId(10);
                existing.setNombre("Álgebra I");
                existing.setHoras(4);
                existing.setAnio("1°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setDespliegue("Segundo Semestre");

                when(materiaRepository.findById(10)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(catedraRepository.existsByMateria_IdAndEstadoIgnoreCase(10, "ACTIVA")).thenReturn(true);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.modificarMateria(10, patchRequest, "admin_global"));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("cátedras activas asociadas"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("modificarMateria - Success: ADMIN_UNIDAD updates subject in own unit")
        void testModificarMateria_AdminUnidad_OwnUnit_Success() {
                Materia existing = new Materia();
                existing.setId(25);
                existing.setNombre("Estructuras de Datos");
                existing.setHoras(4);
                existing.setAnio("2°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1); // userAdminUnidad assigned to unidad1

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Estructuras de Datos Avanzadas");

                when(materiaRepository.findById(25)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidadRecord));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot("Estructuras de Datos Avanzadas", 1, 2020, 25))
                                .thenReturn(false);
                when(materiaRepository.save(anyMateria())).thenAnswer(inv -> inv.getArgument(0));

                MateriaDetailDTO result = materiaService.modificarMateria(25, patchRequest, "admin_unidad_1");

                assertNotNull(result);
                assertEquals("Estructuras de Datos Avanzadas", result.getNombre());
                verify(materiaRepository).save(existing);
        }

        @Test
        @DisplayName("modificarMateria - Forbidden: ADMIN_UNIDAD attempts to update subject in different unit")
        void testModificarMateria_AdminUnidad_DifferentUnit_Forbidden() {
                Materia existing = new Materia();
                existing.setId(10);
                existing.setNombre("Álgebra");
                existing.setUnidad(unidad2); // Belongs to unidad2, while user is assigned to unidad1

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Nuevo Nombre");

                when(materiaRepository.findById(10)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidadRecord));

                AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                                () -> materiaService.modificarMateria(10, patchRequest, "admin_unidad_1"));

                assertTrue(ex.getMessage().contains("No tiene permisos para modificar materias en esta unidad académica"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("modificarMateria - NotFound: Non-existent subject ID")
        void testModificarMateria_NotFound() {
                when(materiaRepository.findById(9999)).thenReturn(Optional.empty());

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Nuevo");

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.modificarMateria(9999, patchRequest, "admin_global"));

                assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("Materia no encontrada"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("modificarMateria - Conflict: Name duplicate in same unit and study plan")
        void testModificarMateria_DuplicateName_Conflict() {
                Materia existing = new Materia();
                existing.setId(11);
                existing.setNombre("Física II");
                existing.setPlanEstudio(2023);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Física I");

                when(materiaRepository.findById(11)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot("Física I", 1, 2023, 11))
                                .thenReturn(true);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> materiaService.modificarMateria(11, patchRequest, "admin_global"));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                assertTrue(Objects.requireNonNull(ex.getReason()).contains("Ya existe otra materia"));
                verify(materiaRepository, never()).save(anyMateria());
        }

        @Test
        @DisplayName("modificarMateria - Success: Same name does not trigger duplicate conflict")
        void testModificarMateria_SameName_NoConflict() {
                Materia existing = new Materia();
                existing.setId(10);
                existing.setNombre("Química General");
                existing.setHoras(4);
                existing.setAnio("1°");
                existing.setDespliegue("Primer Semestre");
                existing.setPlanEstudio(2020);
                existing.setUnidad(unidad1);

                MateriaPatchRequest patchRequest = new MateriaPatchRequest();
                patchRequest.setNombre("Química General");
                patchRequest.setAnio("2°");

                when(materiaRepository.findById(10)).thenReturn(Optional.of(existing));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                when(materiaRepository.save(anyMateria())).thenAnswer(inv -> inv.getArgument(0));

                MateriaDetailDTO result = materiaService.modificarMateria(10, patchRequest, "admin_global");

                assertNotNull(result);
                assertEquals("2°", result.getAnio());
                // Verify existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot was NOT even invoked because name didn't change
                verify(materiaRepository, never()).existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(any(), any(), any(), any());
                verify(materiaRepository).save(existing);
        }

        // ==================== Tests for listarMaterias ====================

        @Test
        @DisplayName("listarMaterias - Paginated for ADMIN_GLOBAL passes null unitId")
        void testListarMaterias_GlobalAdmin_Paginated() {
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));
                Materia m = new Materia();
                m.setId(1);
                m.setNombre("Algoritmos");
                m.setUnidad(unidad1);
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
                org.springframework.data.domain.Page<Materia> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(m));

                when(materiaRepository.searchMaterias(isNull(), eq("Algo"), eq(pageable))).thenReturn(page);

                org.springframework.data.domain.Page<MateriaDetailDTO> result = 
                        materiaService.listarMaterias("Algo", pageable, "admin_global");

                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Algoritmos", result.getContent().get(0).getNombre());
                verify(materiaRepository).searchMaterias(isNull(), eq("Algo"), eq(pageable));
        }

        @Test
        @DisplayName("listarMaterias - Scoped for ADMIN_UNIDAD uses admin's unitId")
        void testListarMaterias_AdminUnidad_List() {
                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                AdminUnidad adminUnidad = new AdminUnidad();
                adminUnidad.setId(200);
                adminUnidad.setUnidad(unidad1);
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidad));

                Materia m = new Materia();
                m.setId(1);
                m.setNombre("Algoritmos");
                m.setUnidad(unidad1);

                when(materiaRepository.searchMateriasList(eq(1), isNull())).thenReturn(java.util.List.of(m));

                java.util.List<MateriaDetailDTO> result = materiaService.listarMaterias(null, "admin_unidad_1");

                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(1, result.get(0).getUnidadId());
                verify(materiaRepository).searchMateriasList(eq(1), isNull());
        }

        // ==================== Tests for obtenerMateriaPorId ====================

        @Test
        @DisplayName("obtenerMateriaPorId - Success for ADMIN_GLOBAL")
        void testObtenerMateriaPorId_Success() {
                Materia m = new Materia();
                m.setId(10);
                m.setNombre("Fisica");
                m.setUnidad(unidad1);

                when(materiaRepository.findById(10)).thenReturn(Optional.of(m));
                when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(userGlobalAdmin));

                MateriaDetailDTO dto = materiaService.obtenerMateriaPorId(10, "admin_global");

                assertNotNull(dto);
                assertEquals(10, dto.getId());
                assertEquals("Fisica", dto.getNombre());
        }

        @Test
        @DisplayName("obtenerMateriaPorId - 403 Forbidden for ADMIN_UNIDAD when subject belongs to another unit")
        void testObtenerMateriaPorId_Forbidden() {
                Materia m = new Materia();
                m.setId(10);
                m.setNombre("Fisica");
                m.setUnidad(unidad2); // Belongs to unit 2

                when(materiaRepository.findById(10)).thenReturn(Optional.of(m));
                when(usuarioRepository.findByUsername("admin_unidad_1")).thenReturn(Optional.of(userAdminUnidad));
                AdminUnidad adminUnidad = new AdminUnidad();
                adminUnidad.setId(200);
                adminUnidad.setUnidad(unidad1); // Admin belongs to unit 1
                when(adminUnidadRepository.findById(200)).thenReturn(Optional.of(adminUnidad));

                AccessDeniedException ex = assertThrows(
                        AccessDeniedException.class,
                        () -> materiaService.obtenerMateriaPorId(10, "admin_unidad_1")
                );

                assertTrue(ex.getMessage().contains("No tiene permisos"));
        }

        @Test
        @DisplayName("obtenerMateriaPorId - 404 Not Found when subject does not exist")
        void testObtenerMateriaPorId_NotFound() {
                when(materiaRepository.findById(9999)).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(
                        ResponseStatusException.class,
                        () -> materiaService.obtenerMateriaPorId(9999, "admin_global")
                );

                assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
}


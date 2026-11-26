package com.atlas.atlas_backend.funcion;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.materia.MateriaRepository;
import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FuncionServiceTest {

    @Mock
    private FuncionRepository funcionRepository;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private EmailService emailService;

    @org.springframework.lang.NonNull
    private static Cargo anyCargo() {
        Cargo c = any(Cargo.class);
        return c != null ? c : new Cargo();
    }

    @org.springframework.lang.NonNull
    private static Docente anyDocente() {
        Docente d = any(Docente.class);
        return d != null ? d : new Docente();
    }

    @InjectMocks
    private FuncionService funcionService;

    private Usuario adminGlobal;
    private Usuario adminUnidadUser;
    private AdminUnidad adminUnidad;
    private Unidad unidadIngenieria;
    private Unidad unidadEconomicas;
    private Cargo cargoAsignado;
    private Designacion designacion;
    private Docente docente;
    private Materia materiaIngenieria;

    @BeforeEach
    void setUp() {
        Rol rolGlobal = new Rol();
        rolGlobal.setNombre("ADMIN_GLOBAL");
        adminGlobal = new Usuario();
        adminGlobal.setId(1);
        adminGlobal.setUsername("admin_global");
        adminGlobal.setRol(rolGlobal);

        unidadIngenieria = new Unidad();
        unidadIngenieria.setUnidadId(10);
        unidadIngenieria.setUnidadNombre("Facultad de Ingeniería");

        unidadEconomicas = new Unidad();
        unidadEconomicas.setUnidadId(20);
        unidadEconomicas.setUnidadNombre("Facultad de Ciencias Económicas");

        Rol rolUnidad = new Rol();
        rolUnidad.setNombre("ADMIN_UNIDAD");
        adminUnidadUser = new Usuario();
        adminUnidadUser.setId(2);
        adminUnidadUser.setUsername("admin_unidad");
        adminUnidadUser.setRol(rolUnidad);

        adminUnidad = new AdminUnidad();
        adminUnidad.setId(2);
        adminUnidad.setUnidad(unidadIngenieria);

        Dedicacion semi = new Dedicacion();
        semi.setDedicacionId(2);
        semi.setDedicacionNombre("Semiexclusiva");
        semi.setDedicacionHoras(20);

        Categoria categoria = new Categoria();
        categoria.setCategoriaNombre("Profesor Adjunto");

        Caracter caracter = new Caracter();
        caracter.setCaracterNombre("Ordinario");

        docente = new Docente();
        docente.setId(30);
        docente.setNombre("Ana");
        docente.setApellido("Gómez");
        docente.setDni(32111222L);
        docente.setCuil(27321112224L);
        docente.setEmailInstitucional("docente@universidad.edu");
        docente.setEstado("Inactivo");
        docente.setActivo(false);

        designacion = new Designacion();
        designacion.setDesignacionId(55);
        designacion.setDesignacionNumeroResolucion("RES-2026-100");
        designacion.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        designacion.setDesignacionFechaFin(LocalDate.of(2027, 2, 28));
        designacion.setDesignacionEstadoActual("Activo");
        designacion.setUsuarioId(30);

        cargoAsignado = new Cargo();
        cargoAsignado.setId(14);
        cargoAsignado.setCodigo(5014L);
        cargoAsignado.setHorasOcupadas(0);
        cargoAsignado.setEstado("Asignado");
        cargoAsignado.setUnidad(unidadIngenieria);
        cargoAsignado.setDedicacion(semi);
        cargoAsignado.setCategoria(categoria);
        cargoAsignado.setCaracter(caracter);
        cargoAsignado.setDesignacion(designacion);

        materiaIngenieria = new Materia();
        materiaIngenieria.setId(3);
        materiaIngenieria.setNombre("Algoritmos y Estructuras de Datos");
        materiaIngenieria.setUnidad(unidadIngenieria);
    }

    @Test
    void asignarFunciones_CargoInexistente_Throws404() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(999999)).thenReturn(Optional.empty());

        AsignarFuncionesCargoRequest req = buildRequest(buildItem("Docencia", "Planta", 10, 3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionService.asignarFuncionesACargo(999999, req, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("El cargo especificado no existe", ex.getReason());
    }

    @Test
    void asignarFunciones_CargoSinDesignacion_Throws400() {
        cargoAsignado.setEstado("Libre");
        cargoAsignado.setDesignacion(null);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(12)).thenReturn(Optional.of(cargoAsignado));

        AsignarFuncionesCargoRequest req = buildRequest(buildItem("Docencia", "Planta", 10, 3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionService.asignarFuncionesACargo(12, req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El cargo debe estar vinculado a una designación", ex.getReason());
    }

    @Test
    void asignarFunciones_AdminUnidadOtraUnidad_Throws403() {
        adminUnidad.setUnidad(unidadIngenieria);
        cargoAsignado.setUnidad(unidadEconomicas);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        AsignarFuncionesCargoRequest req = buildRequest(buildItem("Docencia", "Planta", 10, 3));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> funcionService.asignarFuncionesACargo(14, req, "admin_unidad"));

        assertEquals("No tiene permisos para modificar cargos de otra unidad", ex.getMessage());
        verify(funcionRepository, never()).saveAll(java.util.Objects.requireNonNull(anyList()));
    }

    @Test
    void asignarFunciones_SumaHorasSuperaDedicacion_Throws400AndDoesNotPersist() {
        Dedicacion simple = new Dedicacion();
        simple.setDedicacionNombre("Simple");
        simple.setDedicacionHoras(10);
        cargoAsignado.setDedicacion(simple);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));

        AsignarFuncionesCargoRequest req = buildRequest(
                buildItem("Docencia", "Planta", 6, 3),
                buildItem("Investigación", "Investigación", 8, 3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionService.asignarFuncionesACargo(14, req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("La suma de horas (14) supera la dedicación máxima del cargo (10)", ex.getReason());
        verify(funcionRepository, never()).saveAll(java.util.Objects.requireNonNull(anyList()));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void asignarFunciones_MateriaInexistente_Throws404() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));
        when(materiaRepository.findById(8888)).thenReturn(Optional.empty());

        AsignarFuncionesCargoRequest req = buildRequest(buildItem("Docencia", "Planta", 10, 8888));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionService.asignarFuncionesACargo(14, req, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("La materia especificada no existe", ex.getReason());
    }

    @Test
    void asignarFunciones_MateriaOtraUnidad_Throws400() {
        Materia materiaEconomicas = new Materia();
        materiaEconomicas.setId(99);
        materiaEconomicas.setNombre("Contabilidad I");
        materiaEconomicas.setUnidad(unidadEconomicas);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));
        when(materiaRepository.findById(99)).thenReturn(Optional.of(materiaEconomicas));

        AsignarFuncionesCargoRequest req = buildRequest(buildItem("Docencia", "Planta", 10, 99));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionService.asignarFuncionesACargo(14, req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("La materia no pertenece a la misma unidad académica del cargo", ex.getReason());
        verify(funcionRepository, never()).saveAll(java.util.Objects.requireNonNull(anyList()));
    }

    @Test
    void asignarFunciones_SubOcupacionExitosa_Returns200() throws Exception {
        Dedicacion exclusiva = new Dedicacion();
        exclusiva.setDedicacionNombre("Exclusiva");
        exclusiva.setDedicacionHoras(40);
        cargoAsignado.setDedicacion(exclusiva);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobal));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));
        when(materiaRepository.findById(3)).thenReturn(Optional.of(materiaIngenieria));
        when(funcionRepository.saveAll(java.util.Objects.requireNonNull(anyList()))).thenAnswer(invocation -> {
            List<Funcion> list = invocation.getArgument(0);
            int id = 101;
            for (Funcion f : list) {
                f.setId(id++);
            }
            return list;
        });
        when(docenteRepository.findById(30)).thenReturn(Optional.of(docente));
        when(docenteRepository.save(anyDocente())).thenAnswer(inv -> inv.getArgument(0));

        AsignarFuncionesCargoRequest req = buildRequest(
                buildItem("Docencia Frente a Curso", "Planta", 15, 3),
                buildItem("Extensión Universitaria", "Extensión", 10, 3));

        DesignacionCompletaResponseDTO response = funcionService.asignarFuncionesACargo(14, req, "admin_global");

        assertEquals(14, response.getCargoId());
        assertEquals(40, response.getHorasDedicacion());
        assertEquals(25, response.getHorasOcupadas());
        assertEquals(25, cargoAsignado.getHorasOcupadas());
        assertEquals("Activo", response.getDocenteEstado());
        assertTrue(docente.getActivo());
        assertNull(response.getDocumentoId());
        assertTrue(response.getNotificacionEnviada());
        assertEquals(2, response.getFunciones().size());
        assertEquals("Extensión", response.getFunciones().get(1).getTipo());

        verify(emailService).sendDesignacionCompletaNotification(
                eq("docente@universidad.edu"),
                eq("RES-2026-100"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2027, 2, 28)),
                eq(cargoAsignado),
                anyList());
    }

    @Test
    void asignarFunciones_AdminUnidadMismaUnidad_OcupacionCompleta_Returns200() throws Exception {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(cargoRepository.findById(14)).thenReturn(Optional.of(cargoAsignado));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(materiaRepository.findById(3)).thenReturn(Optional.of(materiaIngenieria));
        when(funcionRepository.saveAll(java.util.Objects.requireNonNull(anyList()))).thenAnswer(invocation -> {
            List<Funcion> list = invocation.getArgument(0);
            int id = 201;
            for (Funcion f : list) {
                f.setId(id++);
            }
            return list;
        });
        when(docenteRepository.findById(30)).thenReturn(Optional.of(docente));
        when(docenteRepository.save(anyDocente())).thenAnswer(inv -> inv.getArgument(0));

        AsignarFuncionesCargoRequest req = buildRequest(
                buildItem("Docencia Frente a Curso", "Planta", 10, 3),
                buildItem("Proyecto de Investigación Aplicada", "Investigación", 10, 3));

        DesignacionCompletaResponseDTO response = funcionService.asignarFuncionesACargo(14, req, "admin_unidad");

        assertEquals(20, response.getHorasOcupadas());
        assertEquals(20, response.getHorasDedicacion());
        assertEquals(55, response.getDesignacionId());
        assertNull(response.getDocumentoId());
        assertEquals("Activo", response.getDocenteEstado());
        assertTrue(response.getNotificacionEnviada());
    }

    private AsignarFuncionesCargoRequest buildRequest(FuncionItemRequest... items) {
        AsignarFuncionesCargoRequest req = new AsignarFuncionesCargoRequest();
        req.setFunciones(List.of(items));
        return req;
    }

    private FuncionItemRequest buildItem(String nombre, String tipo, Integer horas, Integer materiaId) {
        FuncionItemRequest item = new FuncionItemRequest();
        item.setNombre(nombre);
        item.setTipo(tipo);
        item.setHoras(horas);
        item.setMateriaId(materiaId);
        return item;
    }
}

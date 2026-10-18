package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.caracter.CaracterRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.categoria.CategoriaRepository;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.dedicacion.DedicacionRepository;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.origen.OrigenRepository;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import com.atlas.atlas_backend.usuario.*;
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
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CargoServiceTest {

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private OrigenRepository origenRepository;

    @Mock
    private CaracterRepository caracterRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private DedicacionRepository dedicacionRepository;

    @Mock
    private UnidadRepository unidadRepository;

    @InjectMocks
    private CargoService cargoService;

    private Usuario adminGlobalUser;
    private Usuario adminUnidadUser;
    private AdminUnidad adminUnidad;
    private Unidad testUnidad;
    private Origen testOrigen;
    private Caracter testCaracter;
    private Categoria testCategoria;
    private Dedicacion testDedicacion;
    private CargoCreateRequest validRequest;

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
        testUnidad.setUnidadId(10);
        testUnidad.setUnidadNombre("Facultad de Ingeniería");

        adminUnidad = new AdminUnidad();
        adminUnidad.setId(2);
        adminUnidad.setUnidad(testUnidad);

        testOrigen = new Origen();
        testOrigen.setOrigenId(1);
        testOrigen.setOrigenNombre("Tesoro");

        testCaracter = new Caracter();
        testCaracter.setCaracterId(1);
        testCaracter.setCaracterNombre("Ordinario");

        testCategoria = new Categoria();
        testCategoria.setCategoriaId(1);
        testCategoria.setCategoriaNombre("Profesor Titular");

        testDedicacion = new Dedicacion();
        testDedicacion.setDedicacionId(1);
        testDedicacion.setDedicacionNombre("Exclusiva");

        validRequest = new CargoCreateRequest();
        validRequest.setCodigo(5001L);
        validRequest.setFechaCreacion(LocalDate.of(2026, 10, 18));
        validRequest.setOrigenId(1);
        validRequest.setCaracterId(1);
        validRequest.setCategoriaId(1);
        validRequest.setDedicacionId(1);
        validRequest.setUnidadId(10);
    }

    private void mockCatalogueLookups() {
        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(1)).thenReturn(Optional.of(testDedicacion));
        when(unidadRepository.findById(10)).thenReturn(Optional.of(testUnidad));
    }

    @Test
    void crearCargo_AdminGlobal_Success() throws Exception {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        mockCatalogueLookups();
        when(cargoRepository.save(Objects.requireNonNull(any(Cargo.class))))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0)));

        CargoDetailDTO result = cargoService.crearCargo(validRequest, "admin_global");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
        assertEquals("Libre", result.getEstado());
        assertEquals("Profesor Titular", result.getCategoria());
        assertEquals("Exclusiva", result.getDedicacion());
        assertEquals("Ordinario", result.getCaracter());
        assertNull(result.getCargoPredecesor());
        verify(cargoRepository, times(1)).save(Objects.requireNonNull(any(Cargo.class)));
    }

    @Test
    void crearCargo_WithPredecessor_Success() throws Exception {
        validRequest.setPredecesorCodigo(4001L);

        Cargo predecesor = new Cargo();
        predecesor.setId(99);
        predecesor.setCodigo(4001L);
        predecesor.setEstado("Baja");
        predecesor.setCategoria(testCategoria);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        mockCatalogueLookups();
        when(cargoRepository.findByCodigo(4001L)).thenReturn(Optional.of(predecesor));
        when(cargoRepository.save(Objects.requireNonNull(any(Cargo.class))))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0)));

        CargoDetailDTO result = cargoService.crearCargo(validRequest, "admin_global");

        assertNotNull(result);
        assertNotNull(result.getCargoPredecesor());
        assertEquals(4001L, result.getCargoPredecesor().getNumero());
        assertEquals("Baja", result.getCargoPredecesor().getEstado());
        assertEquals("Profesor Titular", result.getCargoPredecesor().getCategoria());
    }

    @Test
    void crearCargo_AdminUnidad_OwnUnit_Success() throws Exception {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        mockCatalogueLookups();
        when(cargoRepository.save(Objects.requireNonNull(any(Cargo.class))))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0)));

        CargoDetailDTO result = cargoService.crearCargo(validRequest, "admin_unidad");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
        assertEquals("Libre", result.getEstado());
    }

    @Test
    void crearCargo_AdminUnidad_CrossUnit_ThrowsAccessDenied() {
        validRequest.setUnidadId(20);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        assertThrows(AccessDeniedException.class, () -> cargoService.crearCargo(validRequest, "admin_unidad"));

        verify(cargoRepository, never()).save(Objects.requireNonNull(any(Cargo.class)));
    }

    @Test
    void crearCargo_DuplicateCodigo_ThrowsConflict() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.crearCargo(validRequest, "admin_global"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("El número de cargo ya existe"));
        verify(cargoRepository, never()).save(Objects.requireNonNull(any(Cargo.class)));
    }

    @Test
    void crearCargo_MissingCatalogueEntity_ThrowsNotFound() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        when(origenRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.crearCargo(validRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Origen no encontrado"));
    }

    @Test
    void crearCargo_MissingPredecessor_ThrowsNotFound() {
        validRequest.setPredecesorCodigo(9999L);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        mockCatalogueLookups();
        when(cargoRepository.findByCodigo(9999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.crearCargo(validRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Cargo predecesor no encontrado"));
    }
}

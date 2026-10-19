package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.caracter.CaracterRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.categoria.CategoriaRepository;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.dedicacion.DedicacionRepository;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.origen.OrigenRepository;
import com.atlas.atlas_backend.funcion.FuncionRepository;
import com.atlas.atlas_backend.unidad.TipoUnidad;
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

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private FuncionRepository funcionRepository;

    @org.springframework.lang.NonNull
    private static Cargo anyCargo() {
        Cargo c = any(Cargo.class);
        return c != null ? c : new Cargo();
    }

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

        TipoUnidad testTipoUnidad = new TipoUnidad();
        testTipoUnidad.setTipoUnidadId(1);
        testTipoUnidad.setTipoUnidadNombre("Facultad");

        testUnidad = new Unidad();
        testUnidad.setUnidadId(10);
        testUnidad.setUnidadNombre("Facultad de Ingeniería");
        testUnidad.setTipoUnidad(testTipoUnidad);

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

    private void mockCatalogueLookupsForUpdate() {
        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(1)).thenReturn(Optional.of(testDedicacion));
    }

    @Test
    void crearCargo_AdminGlobal_Success() throws Exception {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(false);
        mockCatalogueLookups();
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CargoDetailDTO result = cargoService.crearCargo(validRequest, "admin_global");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
        assertEquals("Libre", result.getEstado());
        assertEquals("Profesor Titular", result.getCategoria());
        assertEquals("Exclusiva", result.getDedicacion());
        assertEquals("Ordinario", result.getCaracter());
        assertEquals(1, result.getOrigenId());
        assertEquals("Tesoro", result.getOrigen());
        assertEquals(10, result.getUnidadId());
        assertEquals("Facultad de Ingeniería", result.getUnidadNombre());
        assertEquals("Facultad", result.getTipoUnidadNombre());
        assertNull(result.getCargoPredecesor());
        verify(cargoRepository, times(1)).save(anyCargo());
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
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void crearCargo_DuplicateCodigo_ThrowsConflict() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.existsByCodigo(5001L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.crearCargo(validRequest, "admin_global"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("El número de cargo ya existe"));
        verify(cargoRepository, never()).save(anyCargo());
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

    @Test
    void modificarCargo_AdminGlobal_Success() throws Exception {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setEstado("Libre");
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);
        existingCargo.setOrigen(testOrigen);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        mockCatalogueLookupsForUpdate();
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CargoDetailDTO result = cargoService.modificarCargo(10, updateRequest, "admin_global");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
        assertEquals("Libre", result.getEstado());
        assertEquals(LocalDate.of(2026, 10, 19), result.getFechaCreacion());
        assertEquals("Profesor Titular", result.getCategoria());
        assertEquals("Exclusiva", result.getDedicacion());
        assertEquals("Ordinario", result.getCaracter());
        assertEquals(1, result.getOrigenId());
        assertEquals("Tesoro", result.getOrigen());
        assertEquals(10, result.getUnidadId());
        assertEquals("Facultad de Ingeniería", result.getUnidadNombre());
        assertEquals("Facultad", result.getTipoUnidadNombre());
        verify(cargoRepository, times(1)).save(anyCargo());
    }

    @Test
    void modificarCargo_WithNewPredecessor_Success() throws Exception {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setEstado("Libre");
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);

        Cargo predecesor = new Cargo();
        predecesor.setId(99);
        predecesor.setCodigo(4001L);
        predecesor.setEstado("Baja");
        predecesor.setCategoria(testCategoria);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);
        updateRequest.setPredecesorCodigo(4001L);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        mockCatalogueLookupsForUpdate();
        when(cargoRepository.findByCodigo(4001L)).thenReturn(Optional.of(predecesor));
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CargoDetailDTO result = cargoService.modificarCargo(10, updateRequest, "admin_global");

        assertNotNull(result);
        assertNotNull(result.getCargoPredecesor());
        assertEquals(4001L, result.getCargoPredecesor().getNumero());
        assertEquals("Baja", result.getCargoPredecesor().getEstado());
    }

    @Test
    void modificarCargo_PreserveExistingPredecessor_WhenNullInRequest() throws Exception {
        Cargo predecesor = new Cargo();
        predecesor.setId(99);
        predecesor.setCodigo(4001L);
        predecesor.setEstado("Baja");
        predecesor.setCategoria(testCategoria);

        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setEstado("Libre");
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);
        existingCargo.setCargoPredecesor(predecesor);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);
        updateRequest.setPredecesorCodigo(null);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        mockCatalogueLookupsForUpdate();
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CargoDetailDTO result = cargoService.modificarCargo(10, updateRequest, "admin_global");

        assertNotNull(result);
        assertNotNull(result.getCargoPredecesor());
        assertEquals(4001L, result.getCargoPredecesor().getNumero());
    }

    @Test
    void modificarCargo_AdminUnidad_OwnUnit_Success() throws Exception {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setEstado("Libre");
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        mockCatalogueLookupsForUpdate();
        when(cargoRepository.save(anyCargo()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CargoDetailDTO result = cargoService.modificarCargo(10, updateRequest, "admin_unidad");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
    }

    @Test
    void modificarCargo_AdminUnidad_CrossUnit_ThrowsAccessDenied() {
        Unidad otherUnidad = new Unidad();
        otherUnidad.setUnidadId(20);

        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setUnidad(otherUnidad);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        assertThrows(AccessDeniedException.class, () -> cargoService.modificarCargo(10, updateRequest, "admin_unidad"));

        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void modificarCargo_DedicacionHoursLessThanOccupied_ThrowsBadRequest() {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setHorasOcupadas(20);
        existingCargo.setUnidad(testUnidad);

        Dedicacion lowHoursDedicacion = new Dedicacion();
        lowHoursDedicacion.setDedicacionId(3);
        lowHoursDedicacion.setDedicacionNombre("Simple");
        lowHoursDedicacion.setDedicacionHoras(10);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(3);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(3)).thenReturn(Optional.of(lowHoursDedicacion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.modificarCargo(10, updateRequest, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage()
                .contains("Las horas de la dedicación no pueden ser inferiores a las horas ocupadas"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void modificarCargo_SelfReferencingPredecessor_ThrowsBadRequest() {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);
        updateRequest.setPredecesorCodigo(5001L);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        mockCatalogueLookupsForUpdate();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.modificarCargo(10, updateRequest, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Un cargo no puede ser predecesor de sí mismo"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void modificarCargo_NonExistentCargo_ThrowsNotFound() {
        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);

        when(cargoRepository.findById(9999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.modificarCargo(9999, updateRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Cargo no encontrado"));
    }

    @Test
    void modificarCargo_NonExistentCatalogue_ThrowsNotFound() {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(999);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(origenRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.modificarCargo(10, updateRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Origen no encontrado"));
    }

    @Test
    void modificarCargo_NonExistentPredecessor_ThrowsNotFound() {
        Cargo existingCargo = new Cargo();
        existingCargo.setId(10);
        existingCargo.setCodigo(5001L);
        existingCargo.setHorasOcupadas(0);
        existingCargo.setUnidad(testUnidad);

        CargoUpdateRequest updateRequest = new CargoUpdateRequest();
        updateRequest.setFechaCreacion(LocalDate.of(2026, 10, 19));
        updateRequest.setOrigenId(1);
        updateRequest.setCaracterId(1);
        updateRequest.setCategoriaId(1);
        updateRequest.setDedicacionId(1);
        updateRequest.setPredecesorCodigo(9999L);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(existingCargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        mockCatalogueLookupsForUpdate();
        when(cargoRepository.findByCodigo(9999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cargoService.modificarCargo(10, updateRequest, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Cargo predecesor no encontrado"));
    }

    @Test
    void getCargoDetails_AdminGlobal_Success() throws Exception {
        Cargo cargo = new Cargo();
        cargo.setId(10);
        cargo.setCodigo(5001L);
        cargo.setEstado("Libre");
        cargo.setFechaCreacion(LocalDate.of(2026, 10, 18));
        cargo.setCategoria(testCategoria);
        cargo.setDedicacion(testDedicacion);
        cargo.setCaracter(testCaracter);
        cargo.setOrigen(testOrigen);
        cargo.setUnidad(testUnidad);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(cargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));

        CargoDetailDTO result = cargoService.getCargoDetails(10, "admin_global");

        assertNotNull(result);
        assertEquals(5001L, result.getNumero());
        assertEquals("Libre", result.getEstado());
        assertEquals("Profesor Titular", result.getCategoria());
        assertEquals("Exclusiva", result.getDedicacion());
        assertEquals("Ordinario", result.getCaracter());
        assertEquals(1, result.getOrigenId());
        assertEquals("Tesoro", result.getOrigen());
        assertEquals(10, result.getUnidadId());
        assertEquals("Facultad de Ingeniería", result.getUnidadNombre());
        assertEquals("Facultad", result.getTipoUnidadNombre());
    }

    @Test
    void getCargoDetails_NullOriginAndUnit_Success() throws Exception {
        Cargo cargo = new Cargo();
        cargo.setId(10);
        cargo.setCodigo(5001L);
        cargo.setEstado("Libre");
        cargo.setOrigen(null);
        cargo.setUnidad(null);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(cargo));
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));

        CargoDetailDTO result = cargoService.getCargoDetails(10, "admin_global");

        assertNotNull(result);
        assertNull(result.getOrigenId());
        assertNull(result.getOrigen());
        assertNull(result.getUnidadId());
        assertNull(result.getUnidadNombre());
        assertNull(result.getTipoUnidadNombre());
    }

    @Test
    void getCargoDetails_AdminUnidad_OwnUnit_Success() throws Exception {
        Cargo cargo = new Cargo();
        cargo.setId(10);
        cargo.setCodigo(5001L);
        cargo.setEstado("Libre");
        cargo.setOrigen(testOrigen);
        cargo.setUnidad(testUnidad);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(cargo));
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        CargoDetailDTO result = cargoService.getCargoDetails(10, "admin_unidad");

        assertNotNull(result);
        assertEquals(1, result.getOrigenId());
        assertEquals("Tesoro", result.getOrigen());
        assertEquals(10, result.getUnidadId());
        assertEquals("Facultad de Ingeniería", result.getUnidadNombre());
    }

    @Test
    void getCargoDetails_AdminUnidad_CrossUnit_ThrowsException() {
        Unidad otherUnidad = new Unidad();
        otherUnidad.setUnidadId(20);

        Cargo cargo = new Cargo();
        cargo.setId(10);
        cargo.setUnidad(otherUnidad);

        when(cargoRepository.findById(10)).thenReturn(Optional.of(cargo));
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        Exception exception = assertThrows(Exception.class,
                () -> cargoService.getCargoDetails(10, "admin_unidad"));

        assertTrue(exception.getMessage().contains("No tiene permisos para ver este cargo"));
    }
}

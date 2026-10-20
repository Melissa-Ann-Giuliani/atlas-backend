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
import com.atlas.atlas_backend.designacion.Designacion;
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
        testDedicacion.setDedicacionHoras(40);

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

    // =========================================================================
    // SUBDIVIDIR CARGO TESTS (RF-19)
    // =========================================================================

    private SubdividirCargoRequest buildValidSubdivisionRequest() {
        SubdividirCargoRequest req = new SubdividirCargoRequest();
        req.setCargoOrigenCodigo(8001L);

        SubcargoItemRequest item1 = new SubcargoItemRequest();
        item1.setCodigo(8002L);
        item1.setFechaCreacion(LocalDate.of(2026, 10, 19));
        item1.setOrigenId(1);
        item1.setCaracterId(1);
        item1.setCategoriaId(1);
        item1.setDedicacionId(1); // 20 hs
        item1.setUnidadId(10);

        SubcargoItemRequest item2 = new SubcargoItemRequest();
        item2.setCodigo(8003L);
        item2.setFechaCreacion(LocalDate.of(2026, 10, 19));
        item2.setOrigenId(1);
        item2.setCaracterId(1);
        item2.setCategoriaId(1);
        item2.setDedicacionId(1); // 20 hs
        item2.setUnidadId(10);

        req.setSubcargos(java.util.List.of(item1, item2));
        return req;
    }

    @Test
    void subdividirCargo_Success_HappyPath() throws Exception {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        Dedicacion dedParent = new Dedicacion();
        dedParent.setDedicacionId(2);
        dedParent.setDedicacionNombre("Exclusiva");
        dedParent.setDedicacionHoras(40);

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Licencia");
        parent.setHorasOcupadas(0);
        parent.setDedicacion(dedParent);
        parent.setUnidad(testUnidad);

        Dedicacion dedSub = new Dedicacion();
        dedSub.setDedicacionId(1);
        dedSub.setDedicacionNombre("Semiexclusiva");
        dedSub.setDedicacionHoras(20);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));
        when(cargoRepository.existsByCodigo(8002L)).thenReturn(false);
        when(cargoRepository.existsByCodigo(8003L)).thenReturn(false);

        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(1)).thenReturn(Optional.of(dedSub));
        when(unidadRepository.findById(10)).thenReturn(Optional.of(testUnidad));

        when(cargoRepository.save(anyCargo())).thenAnswer(invocation -> invocation.getArgument(0));

        java.util.List<CargoDetailDTO> result = cargoService.subdividirCargo(req, "admin_global");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(8002L, result.get(0).getNumero());
        assertEquals(8003L, result.get(1).getNumero());
        assertEquals("Libre", result.get(0).getEstado());
        assertEquals("Libre", result.get(1).getEstado());

        // Verify parent hours updated to 100% (40)
        assertEquals(40, parent.getHorasOcupadas());
        assertEquals("Licencia", parent.getEstado());
        verify(cargoRepository, times(3)).save(anyCargo()); // 1 parent + 2 subcargos
    }

    @Test
    void subdividirCargo_HoursExceeded_ThrowsBadRequest() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        Dedicacion dedParent = new Dedicacion();
        dedParent.setDedicacionId(2);
        dedParent.setDedicacionNombre("Semiexclusiva");
        dedParent.setDedicacionHoras(20);

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Libre");
        parent.setHorasOcupadas(0);
        parent.setDedicacion(dedParent);
        parent.setUnidad(testUnidad);

        Dedicacion dedSub = new Dedicacion();
        dedSub.setDedicacionId(1);
        dedSub.setDedicacionNombre("Semiexclusiva");
        dedSub.setDedicacionHoras(20); // Each subcargo is 20 -> sum 40 > parent 20

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));
        when(cargoRepository.existsByCodigo(anyLong())).thenReturn(false);

        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(1)).thenReturn(Optional.of(dedSub));
        when(unidadRepository.findById(10)).thenReturn(Optional.of(testUnidad));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("supera las horas del cargo original"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void subdividirCargo_InvalidParentState_ThrowsBadRequest() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Asignado"); // Not Libre or Licencia
        parent.setUnidad(testUnidad);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Solo cargos en estado Libre o Licencia"));
    }

    @Test
    void subdividirCargo_SubcargoCodeAlreadyExists_ThrowsConflict() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Libre");
        parent.setUnidad(testUnidad);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));
        when(cargoRepository.existsByCodigo(8002L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("ya existe"));
    }

    @Test
    void subdividirCargo_DuplicateCodeInPayload_ThrowsConflict() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();
        req.getSubcargos().get(1).setCodigo(8002L); // Same as item 1

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("duplicado en la solicitud"));
    }

    @Test
    void subdividirCargo_SubcargosOutOfBounds_ThrowsBadRequest() {
        SubdividirCargoRequest req = new SubdividirCargoRequest();
        req.setCargoOrigenCodigo(8001L);
        req.setSubcargos(java.util.List.of(new SubcargoItemRequest())); // only 1

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("entre 2 y 4"));
    }

    @Test
    void subdividirCargo_ParentNotFound_ThrowsNotFound() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.subdividirCargo(req, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Cargo padre no encontrado"));
    }

    @Test
    void subdividirCargo_AdminUnidad_CrossUnitParent_ThrowsForbidden() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();

        Unidad otherUnidad = new Unidad();
        otherUnidad.setUnidadId(99);

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Libre");
        parent.setUnidad(otherUnidad); // Different unit

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad)); // unit 10
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));

        assertThrows(AccessDeniedException.class,
                () -> cargoService.subdividirCargo(req, "admin_unidad"));
    }

    @Test
    void subdividirCargo_AdminUnidad_CrossUnitSubcargo_ThrowsForbidden() {
        SubdividirCargoRequest req = buildValidSubdivisionRequest();
        req.getSubcargos().get(1).setUnidadId(99); // Cross unit subcargo

        Cargo parent = new Cargo();
        parent.setId(100);
        parent.setCodigo(8001L);
        parent.setEstado("Libre");
        parent.setUnidad(testUnidad); // Unit 10 matches

        Dedicacion dedSub = new Dedicacion();
        dedSub.setDedicacionId(1);
        dedSub.setDedicacionNombre("Semiexclusiva");
        dedSub.setDedicacionHoras(20);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad)); // unit 10
        when(cargoRepository.findByCodigo(8001L)).thenReturn(Optional.of(parent));
        when(cargoRepository.existsByCodigo(anyLong())).thenReturn(false);

        // First item passes permission check and loads catalogs
        when(origenRepository.findById(1)).thenReturn(Optional.of(testOrigen));
        when(caracterRepository.findById(1)).thenReturn(Optional.of(testCaracter));
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(testCategoria));
        when(dedicacionRepository.findById(1)).thenReturn(Optional.of(dedSub));
        when(unidadRepository.findById(10)).thenReturn(Optional.of(testUnidad));

        assertThrows(AccessDeniedException.class,
                () -> cargoService.subdividirCargo(req, "admin_unidad"));
    }

    // --- RF-20: Baja de Cargo Tests ---

    @Test
    void darDeBajaCargo_Scenario1_Success_StandAloneCargo_ReturnsUpdatedCargo() throws Exception {
        Cargo cargo = new Cargo();
        cargo.setId(101);
        cargo.setCodigo(5001L);
        cargo.setEstado("Libre");
        cargo.setHorasOcupadas(40);
        cargo.setCategoria(testCategoria);
        cargo.setDedicacion(testDedicacion);
        cargo.setCaracter(testCaracter);
        cargo.setOrigen(testOrigen);
        cargo.setUnidad(testUnidad);
        cargo.setDesignacion(null);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(101)).thenReturn(Optional.of(cargo));
        when(cargoRepository.findByCargoOrigenDivisionId(101)).thenReturn(java.util.Collections.emptyList());
        when(cargoRepository.save(anyCargo())).thenAnswer(invocation -> invocation.getArgument(0));
        when(funcionRepository.findByCargoIdIn(anyList())).thenReturn(java.util.Collections.emptyList());
        when(cargoRepository.findByCargoPredecesorId(101)).thenReturn(java.util.Collections.emptyList());

        CargoDetailDTO result = cargoService.darDeBajaCargo(101, "admin_global");

        assertNotNull(result);
        assertEquals("Dado de baja", result.getEstado());
        assertEquals("Dado de baja", cargo.getEstado());
        assertEquals(0, cargo.getHorasOcupadas());
        verify(cargoRepository, times(1)).save(cargo);
    }

    @Test
    void darDeBajaCargo_Scenario2_Success_FinalizedDesignation_ReturnsUpdatedCargo() throws Exception {
        Designacion des = new Designacion();
        des.setDesignacionId(1);
        des.setDesignacionNumeroResolucion(1234);
        des.setDesignacionEstadoActual("Finalizada");
        des.setDesignacionFechaInicio(LocalDate.now().minusMonths(6));
        des.setDesignacionFechaFin(LocalDate.now().minusDays(1));

        Cargo cargo = new Cargo();
        cargo.setId(102);
        cargo.setCodigo(5002L);
        cargo.setEstado("Libre");
        cargo.setHorasOcupadas(0);
        cargo.setCategoria(testCategoria);
        cargo.setDedicacion(testDedicacion);
        cargo.setCaracter(testCaracter);
        cargo.setOrigen(testOrigen);
        cargo.setUnidad(testUnidad);
        cargo.setDesignacion(des);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(102)).thenReturn(Optional.of(cargo));
        when(cargoRepository.findByCargoOrigenDivisionId(102)).thenReturn(java.util.Collections.emptyList());
        when(cargoRepository.save(anyCargo())).thenAnswer(invocation -> invocation.getArgument(0));
        when(funcionRepository.findByCargoIdIn(anyList())).thenReturn(java.util.Collections.emptyList());
        when(cargoRepository.findByCargoPredecesorId(102)).thenReturn(java.util.Collections.emptyList());

        CargoDetailDTO result = cargoService.darDeBajaCargo(102, "admin_global");

        assertNotNull(result);
        assertEquals("Dado de baja", result.getEstado());
        assertEquals("Dado de baja", cargo.getEstado());
        verify(cargoRepository, times(1)).save(cargo);
    }

    @Test
    void darDeBajaCargo_Scenario3_Error_ActiveDesignation_ThrowsBadRequest() {
        Designacion des = new Designacion();
        des.setDesignacionId(1);
        des.setDesignacionNumeroResolucion(1234);
        des.setDesignacionEstadoActual("Activa");
        des.setDesignacionFechaInicio(LocalDate.now().minusMonths(1));
        des.setDesignacionFechaFin(LocalDate.now().plusMonths(5));

        Cargo cargo = new Cargo();
        cargo.setId(103);
        cargo.setCodigo(5003L);
        cargo.setEstado("Asignado");
        cargo.setDesignacion(des);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(103)).thenReturn(Optional.of(cargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.darDeBajaCargo(103, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("designación vigente"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void darDeBajaCargo_Scenario4_Error_CargoNotFound_ThrowsNotFound() {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(99999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.darDeBajaCargo(99999, "admin_global"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void darDeBajaCargo_Scenario5_Error_AlreadyDecommissioned_ThrowsBadRequest() {
        Cargo cargo = new Cargo();
        cargo.setId(104);
        cargo.setCodigo(5004L);
        cargo.setEstado("Dado de baja");

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(104)).thenReturn(Optional.of(cargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.darDeBajaCargo(104, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("ya se encuentra dado de baja"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void darDeBajaCargo_Scenario6_Error_AdminUnidad_ThrowsForbidden() {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));

        assertThrows(AccessDeniedException.class,
                () -> cargoService.darDeBajaCargo(101, "admin_unidad"));

        verify(cargoRepository, never()).findById(anyInt());
    }

    @Test
    void darDeBajaCargo_Scenario7_Error_ActiveSubcargosFromDivision_ThrowsBadRequest() {
        Cargo parent = new Cargo();
        parent.setId(105);
        parent.setCodigo(5005L);
        parent.setEstado("Libre");

        Cargo subcargo = new Cargo();
        subcargo.setId(106);
        subcargo.setCodigo(5006L);
        subcargo.setEstado("Libre");
        subcargo.setCargoOrigenDivision(parent);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(105)).thenReturn(Optional.of(parent));
        when(cargoRepository.findByCargoOrigenDivisionId(105)).thenReturn(java.util.List.of(subcargo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cargoService.darDeBajaCargo(105, "admin_global"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("posee subcargos activos"));
        verify(cargoRepository, never()).save(anyCargo());
    }

    @Test
    void darDeBajaCargo_SubcargosFromDivisionDecommissioned_Success() throws Exception {
        Cargo parent = new Cargo();
        parent.setId(105);
        parent.setCodigo(5005L);
        parent.setEstado("Libre");
        parent.setCategoria(testCategoria);
        parent.setDedicacion(testDedicacion);
        parent.setCaracter(testCaracter);
        parent.setOrigen(testOrigen);
        parent.setUnidad(testUnidad);

        Cargo subcargo = new Cargo();
        subcargo.setId(106);
        subcargo.setCodigo(5006L);
        subcargo.setEstado("Dado de baja");
        subcargo.setCargoOrigenDivision(parent);

        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(cargoRepository.findById(105)).thenReturn(Optional.of(parent));
        when(cargoRepository.findByCargoOrigenDivisionId(105)).thenReturn(java.util.List.of(subcargo));
        when(cargoRepository.save(anyCargo())).thenAnswer(invocation -> invocation.getArgument(0));
        when(funcionRepository.findByCargoIdIn(anyList())).thenReturn(java.util.Collections.emptyList());
        when(cargoRepository.findByCargoPredecesorId(105)).thenReturn(java.util.Collections.emptyList());

        CargoDetailDTO result = cargoService.darDeBajaCargo(105, "admin_global");

        assertNotNull(result);
        assertEquals("Dado de baja", result.getEstado());
        assertEquals(0, parent.getHorasOcupadas());
    }
}

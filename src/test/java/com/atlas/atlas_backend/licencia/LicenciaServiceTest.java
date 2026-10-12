package com.atlas.atlas_backend.licencia;

import com.atlas.atlas_backend.archivo.Archivo;
import com.atlas.atlas_backend.archivo.ArchivoRepository;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LicenciaServiceTest {

    @Mock
    private LicenciaRepository licenciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private ArchivoRepository archivoRepository;

    @InjectMocks
    private LicenciaService licenciaService;

    private Usuario adminGlobalUser;
    private Usuario adminUnidadUser;
    private AdminUnidad adminUnidad;
    private Unidad unidadMatematica;
    private Unidad unidadHistoria;
    private Licencia licencia;
    private Designacion designacion;
    private MotivoLicencia motivo;
    private Docente docente;
    private Cargo cargo;

    @BeforeEach
    void setUp() {
        Rol rolGlobal = new Rol();
        rolGlobal.setNombre("Administrador Global");

        adminGlobalUser = new Usuario();
        adminGlobalUser.setId(1);
        adminGlobalUser.setUsername("admin_global");
        adminGlobalUser.setRol(rolGlobal);

        Rol rolUnidad = new Rol();
        rolUnidad.setNombre("Administrador de Unidad");

        adminUnidadUser = new Usuario();
        adminUnidadUser.setId(2);
        adminUnidadUser.setUsername("admin_unidad");
        adminUnidadUser.setRol(rolUnidad);

        unidadMatematica = new Unidad();
        unidadMatematica.setUnidadId(10);
        unidadMatematica.setUnidadNombre("Departamento de Matemática");

        unidadHistoria = new Unidad();
        unidadHistoria.setUnidadId(20);
        unidadHistoria.setUnidadNombre("Departamento de Historia");

        adminUnidad = new AdminUnidad();
        adminUnidad.setId(2);
        adminUnidad.setUnidad(unidadMatematica);

        motivo = new MotivoLicencia();
        motivo.setId(1);
        motivo.setNombre("Maternidad / Paternidad");
        motivo.setDiasCorresp(90);

        designacion = new Designacion();
        designacion.setDesignacionId(100);
        designacion.setDesignacionNumeroResolucion(4500);
        designacion.setDesignacionFechaInicio(LocalDate.of(2026, 1, 1));
        designacion.setDesignacionFechaFin(LocalDate.of(2026, 12, 31));
        designacion.setDesignacionEstadoActual("Licencia");
        designacion.setUsuarioId(50);

        licencia = new Licencia();
        licencia.setId(1);
        licencia.setNroResolucion("RES-2026-100");
        licencia.setFechaInicio(LocalDate.of(2026, 5, 1));
        licencia.setFechaFin(LocalDate.of(2026, 7, 31));
        licencia.setCantidadDias(91);
        licencia.setMotivo(motivo);
        licencia.setDesignacion(designacion);

        docente = new Docente();
        docente.setId(50);
        docente.setNombre("Pablo");
        docente.setApellido("Gutierrez");
        docente.setDni(40111222L);
        docente.setCuil(20401112224L);
        docente.setEmailInstitucional("pablo.gutierrez@atlas.edu");

        Categoria cat = new Categoria();
        cat.setCategoriaNombre("Profesor Adjunto");

        Dedicacion ded = new Dedicacion();
        ded.setDedicacionNombre("Exclusiva");

        cargo = new Cargo();
        cargo.setId(200);
        cargo.setCodigo(7001L);
        cargo.setUnidad(unidadMatematica);
        cargo.setDesignacion(designacion);
        cargo.setCategoria(cat);
        cargo.setDedicacion(ded);
        cargo.setEstado("Licencia");
    }

    @Test
    void getActiveLicencias_GlobalAdmin_CanQueryAcrossAllUnits() throws Exception {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));

        LicenciaListDTO dto = mock(LicenciaListDTO.class);

        Page<LicenciaListDTO> pageResult = new PageImpl<>(List.of(dto));
        when(licenciaRepository.findActiveLicencias(eq(null), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        Pageable pageable = PageRequest.of(0, 10);
        Page<LicenciaListDTO> result = licenciaService.getActiveLicencias(
                "admin_global", null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(licenciaRepository).findActiveLicencias(isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getActiveLicencias_UnitAdmin_RestrictedToOwnUnit() throws Exception {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));

        Page<LicenciaListDTO> pageResult = new PageImpl<>(List.of());
        when(licenciaRepository.findActiveLicencias(eq(10), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        Pageable pageable = PageRequest.of(0, 10);
        Page<LicenciaListDTO> result = licenciaService.getActiveLicencias(
                "admin_unidad", "Gutierrez", 1, null, null, null, pageable);

        assertNotNull(result);
        verify(licenciaRepository).findActiveLicencias(eq(10), eq("Gutierrez"), eq(1), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getLicenciaDetails_Success() throws Exception {
        when(usuarioRepository.findByUsername("admin_global")).thenReturn(Optional.of(adminGlobalUser));
        when(licenciaRepository.findById(1)).thenReturn(Optional.of(licencia));
        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of(cargo));
        when(docenteRepository.findById(50)).thenReturn(Optional.of(docente));

        Archivo archivo = new Archivo();
        archivo.setArchivoId(10);
        archivo.setNombreOriginal("resolucion.pdf");
        archivo.setNombreEncriptado("enc_10.pdf");
        archivo.setFechaSubida(LocalDateTime.now());
        archivo.setTamano(1024L);
        archivo.setTipoContenido("application/pdf");

        when(archivoRepository.findByTipoTramiteAndTramiteId("LICENCIA", 1)).thenReturn(List.of(archivo));

        LicenciaDetailDTO detail = licenciaService.getLicenciaDetails(1, "admin_global");

        assertNotNull(detail);
        assertEquals(1, detail.getId());
        assertEquals("RES-2026-100", detail.getNroResolucion());
        assertEquals("Maternidad / Paternidad", detail.getMotivoNombre());
        assertEquals("Gutierrez, Pablo", detail.getDocenteNombreCompleto());
        assertEquals(40111222L, detail.getDocenteDni());
        assertEquals("Departamento de Matemática", detail.getUnidadNombre());
        assertEquals(1, detail.getCargosDetalle().size());
        assertEquals(1, detail.getDocumentacionAdjunta().size());
        assertEquals("resolucion.pdf", detail.getDocumentacionAdjunta().get(0).getNombreOriginal());
    }

    @Test
    void getLicenciaDetails_UnitAdmin_ThrowsAccessDeniedForOtherUnit() throws Exception {
        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(adminUnidadUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(licenciaRepository.findById(1)).thenReturn(Optional.of(licencia));

        // The cargo is in Historia (unit 20), but user is in Matemática (unit 10)
        Cargo foreignCargo = new Cargo();
        foreignCargo.setId(300);
        foreignCargo.setUnidad(unidadHistoria);
        foreignCargo.setDesignacion(designacion);

        when(cargoRepository.findByDesignacionDesignacionIdIn(List.of(100))).thenReturn(List.of(foreignCargo));

        assertThrows(AccessDeniedException.class, () ->
                licenciaService.getLicenciaDetails(1, "admin_unidad"));
    }

    @Test
    void getLicenciaDetails_NotFound_ThrowsNoSuchElementException() {
        when(licenciaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                licenciaService.getLicenciaDetails(999, "admin_global"));
    }
}

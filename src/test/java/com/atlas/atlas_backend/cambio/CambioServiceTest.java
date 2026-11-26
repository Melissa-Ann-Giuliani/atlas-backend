package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminGlobal;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.Auditor;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CambioServiceTest {

    @Mock
    private CambioRepository cambioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private CambioService cambioService;

    @BeforeEach
    void setUp() {
        cambioService = new CambioService(cambioRepository, usuarioRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarCambio_ModificacionConMetadataCompleta_PersisteCambio() {
        Usuario resp = new Usuario();
        resp.setId(1);
        resp.setUsername("admin1");

        Docente docente = new Docente();
        docente.setId(10);
        docente.setNombre("Juan");
        docente.setApellido("Perez");

        Unidad unidad = new Unidad();
        unidad.setUnidadId(2);

        cambioService.registrarCambio("docentes", "domicilio", "Calle A", "Calle B",
                TipoOperacion.MODIFICACION, resp, docente, unidad);

        ArgumentCaptor<Cambio> captor = ArgumentCaptor.forClass(Cambio.class);
        verify(cambioRepository).save(captor.capture());

        Cambio saved = captor.getValue();
        assertEquals("docentes", saved.getTablaAfectada());
        assertEquals("domicilio", saved.getCampoAfectado());
        assertEquals("Calle A", saved.getValorAnterior());
        assertEquals("Calle B", saved.getValorNuevo());
        assertEquals(TipoOperacion.MODIFICACION, saved.getTipoOperacion());
        assertNotNull(saved.getFechaHora());
        assertEquals(resp, saved.getUsuario());
        assertEquals(docente, saved.getUsuarioAfectado());
        assertEquals(unidad, saved.getUnidad());
    }

    @Test
    void registrarCambio_AltaConValorAnteriorNull_PersisteSinNpe() {
        Usuario resp = new Usuario();
        resp.setId(1);

        cambioService.registrarCambio("designaciones", "registro", null, "RES-123/2026",
                TipoOperacion.ALTA, resp, null, null);

        ArgumentCaptor<Cambio> captor = ArgumentCaptor.forClass(Cambio.class);
        verify(cambioRepository).save(captor.capture());

        Cambio saved = captor.getValue();
        assertEquals(TipoOperacion.ALTA, saved.getTipoOperacion());
        assertNull(saved.getValorAnterior());
        assertEquals("RES-123/2026", saved.getValorNuevo());
    }

    @Test
    void registrarCambio_BajaConValorNuevoNull_PersisteSinNpe() {
        Usuario resp = new Usuario();
        resp.setId(1);

        cambioService.registrarCambio("cargos", "estado", "Libre", null,
                TipoOperacion.BAJA, resp, null, null);

        ArgumentCaptor<Cambio> captor = ArgumentCaptor.forClass(Cambio.class);
        verify(cambioRepository).save(captor.capture());

        Cambio saved = captor.getValue();
        assertEquals(TipoOperacion.BAJA, saved.getTipoOperacion());
        assertEquals("Libre", saved.getValorAnterior());
        assertNull(saved.getValorNuevo());
    }

    @Test
    void registrarCambio_ModificacionSinCambios_NoPersiste() {
        Usuario resp = new Usuario();
        resp.setId(1);

        cambioService.registrarCambio("docentes", "domicilio", " Calle A ", "Calle A",
                TipoOperacion.MODIFICACION, resp, null, null);

        verify(cambioRepository, never()).save(any());
    }

    @Test
    void mapCategoria_DerivaCorrectamenteDesdeTabla() {
        assertEquals("Docente", CambioService.mapCategoria("docentes"));
        assertEquals("Designación", CambioService.mapCategoria("designaciones"));
        assertEquals("Cargo", CambioService.mapCategoria("cargos"));
        assertEquals("Usuario", CambioService.mapCategoria("admin_de_unidad"));
        assertEquals("Materia", CambioService.mapCategoria("materias"));
    }

    @Test
    void consultarHistorial_RangoFechasInvalido_Lanza400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                cambioService.consultarHistorial(
                        null,
                        LocalDate.of(2026, 5, 10),
                        LocalDate.of(2026, 5, 1),
                        null, null, null, null,
                        PageRequest.of(0, 20)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarHistorial_AdminGlobalYAuditor_MapeanRespuestaCorrectamente() {
        Auditor auditor = new Auditor();
        auditor.setId(5);
        auditor.setUsername("auditor1");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor1", "pass", List.of()));
        when(usuarioRepository.findByUsername("auditor1")).thenReturn(Optional.of(auditor));

        Usuario resp = new AdminGlobal();
        resp.setId(1);
        resp.setUsername("admin");
        resp.setNombre("Ana");
        resp.setApellido("Gomez");

        Docente afectado = new Docente();
        afectado.setId(10);
        afectado.setNombre("Juan");
        afectado.setApellido("Perez");

        Unidad unidad = new Unidad();
        unidad.setUnidadId(3);

        Cambio cambio = new Cambio();
        cambio.setId(100);
        cambio.setTablaAfectada("docentes");
        cambio.setCampoAfectado("domicilio");
        cambio.setValorAnterior("A");
        cambio.setValorNuevo("B");
        cambio.setFechaHora(LocalDateTime.of(2026, 5, 1, 10, 30));
        cambio.setTipoOperacion(TipoOperacion.MODIFICACION);
        cambio.setUsuario(resp);
        cambio.setUsuarioAfectado(afectado);
        cambio.setUnidad(unidad);

        when(cambioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cambio)));

        Page<CambioResponseDTO> result = cambioService.consultarHistorial(
                null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        CambioResponseDTO dto = result.getContent().get(0);
        assertEquals(100, dto.getId());
        assertEquals("Docente", dto.getCategoria());
        assertEquals("MODIFICACION", dto.getTipoOperacion());
        assertEquals(10, dto.getUsuarioAfectadoId());
        assertEquals("Perez, Juan", dto.getUsuarioAfectadoNombreCompleto());
        assertEquals(3, dto.getUnidadId());
        assertEquals("admin", dto.getUsuarioResponsable().getUsername());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarHistorial_AdminUnidad_AplicaScopeDeUnidad() {
        Unidad unidad = new Unidad();
        unidad.setUnidadId(7);

        AdminUnidad adminUnidad = new AdminUnidad();
        adminUnidad.setId(2);
        adminUnidad.setUsername("admin_u7");
        adminUnidad.setUnidad(unidad);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin_u7", "pass", List.of()));
        when(usuarioRepository.findByUsername("admin_u7")).thenReturn(Optional.of(adminUnidad));
        when(cambioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<CambioResponseDTO> result = cambioService.consultarHistorial(
                null, null, null, null, null, null, 99, PageRequest.of(0, 20));

        assertTrue(result.isEmpty());
        verify(cambioRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarHistorial_Unpaged_UsaPageableUnpagedConOrdenPorDefecto() {
        when(cambioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<CambioResponseDTO> result = cambioService.consultarHistorial(
                null, null, null, null, null, null, null, Pageable.unpaged());

        assertTrue(result.isEmpty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cambioRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertTrue(captured.isUnpaged());
        assertTrue(captured.getSort().isSorted());
    }
}

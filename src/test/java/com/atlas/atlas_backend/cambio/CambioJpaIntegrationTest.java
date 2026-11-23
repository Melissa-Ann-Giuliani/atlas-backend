package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.unidad.TipoUnidad;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminGlobal;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.Auditor;
import com.atlas.atlas_backend.usuario.CargoAdminGlobal;
import com.atlas.atlas_backend.usuario.CargoAdminUnidad;
import com.atlas.atlas_backend.usuario.CargoAuditor;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.Permiso;
import com.atlas.atlas_backend.usuario.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class CambioJpaIntegrationTest {

    @Autowired
    private CambioService cambioService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void consultarHistorial_FiltrosYRbacPorUnidadYRol_EndToEnd() {
        record Fixture(Integer adminGlobalId, Integer unidad1Id, Integer unidad2Id) {}

        Fixture fixture = transactionTemplate.execute(status -> {
            Permiso permiso = new Permiso();
            permiso.setNombre("AUDIT_TEST_PERM");
            entityManager.persist(permiso);

            Rol rolGlobal = new Rol();
            rolGlobal.setNombre("ADMIN_GLOBAL");
            rolGlobal.setPermisos(Set.of(permiso));
            entityManager.persist(rolGlobal);

            Rol rolUnidad = new Rol();
            rolUnidad.setNombre("ADMIN_UNIDAD");
            rolUnidad.setPermisos(Set.of(permiso));
            entityManager.persist(rolUnidad);

            Rol rolAuditor = new Rol();
            rolAuditor.setNombre("AUDITOR");
            rolAuditor.setPermisos(Set.of(permiso));
            entityManager.persist(rolAuditor);

            Rol rolDocente = new Rol();
            rolDocente.setNombre("DOCENTE");
            rolDocente.setPermisos(Set.of(permiso));
            entityManager.persist(rolDocente);

            TipoUnidad tipoUnidad = new TipoUnidad();
            tipoUnidad.setNombre("Facultad_AuditTest");
            entityManager.persist(tipoUnidad);

            Unidad u1 = new Unidad();
            u1.setUnidadNombre("Unidad Uno Audit");
            u1.setTipoUnidad(tipoUnidad);
            entityManager.persist(u1);

            Unidad u2 = new Unidad();
            u2.setUnidadNombre("Unidad Dos Audit");
            u2.setTipoUnidad(tipoUnidad);
            entityManager.persist(u2);

            AdminGlobal adminGlobal = new AdminGlobal();
            adminGlobal.setUsername("ag_audit_test");
            adminGlobal.setContrasenia("pass");
            adminGlobal.setCorreo("ag_audit@test.com");
            adminGlobal.setNombre("Global");
            adminGlobal.setApellido("Admin");
            adminGlobal.setActivo(true);
            adminGlobal.setRol(rolGlobal);
            adminGlobal.setCargoNombre(CargoAdminGlobal.DESPACHO);
            entityManager.persist(adminGlobal);

            AdminUnidad adminU1 = new AdminUnidad();
            adminU1.setUsername("au1_audit_test");
            adminU1.setContrasenia("pass");
            adminU1.setCorreo("au1_audit@test.com");
            adminU1.setNombre("Unidad1");
            adminU1.setApellido("Admin");
            adminU1.setActivo(true);
            adminU1.setRol(rolUnidad);
            adminU1.setCargoNombre(CargoAdminUnidad.DIRECTOR);
            adminU1.setUnidad(u1);
            entityManager.persist(adminU1);

            Auditor auditor = new Auditor();
            auditor.setUsername("aud_audit_test");
            auditor.setContrasenia("pass");
            auditor.setCorreo("aud_audit@test.com");
            auditor.setNombre("Carlos");
            auditor.setApellido("Auditor");
            auditor.setActivo(true);
            auditor.setRol(rolAuditor);
            auditor.setCargoNombre(CargoAuditor.DECANO);
            entityManager.persist(auditor);

            Docente doc1 = new Docente();
            doc1.setUsername("doc1_audit_test");
            doc1.setContrasenia("pass");
            doc1.setCorreo("doc1_audit@test.com");
            doc1.setEmailInstitucional("doc1_audit@test.com");
            doc1.setNombre("Juan");
            doc1.setApellido("Perez");
            doc1.setActivo(true);
            doc1.setRol(rolDocente);
            doc1.setDni(30111222L);
            doc1.setCuil(20301112221L);
            doc1.setFechaIngreso(LocalDate.of(2020, 1, 1));
            doc1.setEstado("Activo");
            entityManager.persist(doc1);

            Docente doc2 = new Docente();
            doc2.setUsername("doc2_audit_test");
            doc2.setContrasenia("pass");
            doc2.setCorreo("doc2_audit@test.com");
            doc2.setEmailInstitucional("doc2_audit@test.com");
            doc2.setNombre("Maria");
            doc2.setApellido("Gomez");
            doc2.setActivo(true);
            doc2.setRol(rolDocente);
            doc2.setDni(30222333L);
            doc2.setCuil(27302223331L);
            doc2.setFechaIngreso(LocalDate.of(2021, 1, 1));
            doc2.setEstado("Activo");
            entityManager.persist(doc2);

            // Cambio 1: Docente in U1 (MODIFICACION)
            Cambio c1 = new Cambio();
            c1.setTablaAfectada("docentes");
            c1.setCampoAfectado("domicilio");
            c1.setValorAnterior("Calle 1");
            c1.setValorNuevo("Calle 2");
            c1.setFechaHora(LocalDateTime.of(2026, 3, 10, 9, 0));
            c1.setTipoOperacion(TipoOperacion.MODIFICACION);
            c1.setUsuario(adminGlobal);
            c1.setUsuarioAfectado(doc1);
            c1.setUnidad(u1);
            entityManager.persist(c1);

            // Cambio 2: Materia in U1 (ALTA, usuarioAfectado = null) -> should NOT be visible to ADMIN_UNIDAD per B2
            Cambio c2 = new Cambio();
            c2.setTablaAfectada("materias");
            c2.setCampoAfectado("registro");
            c2.setValorAnterior(null);
            c2.setValorNuevo("Algebra I");
            c2.setFechaHora(LocalDateTime.of(2026, 3, 11, 10, 0));
            c2.setTipoOperacion(TipoOperacion.ALTA);
            c2.setUsuario(adminU1);
            c2.setUsuarioAfectado(null);
            c2.setUnidad(u1);
            entityManager.persist(c2);

            // Cambio 3: Docente in U2 (BAJA)
            Cambio c3 = new Cambio();
            c3.setTablaAfectada("docentes");
            c3.setCampoAfectado("docente_estado");
            c3.setValorAnterior("Activo");
            c3.setValorNuevo("Inactivo");
            c3.setFechaHora(LocalDateTime.of(2026, 3, 12, 15, 30));
            c3.setTipoOperacion(TipoOperacion.BAJA);
            c3.setUsuario(adminGlobal);
            c3.setUsuarioAfectado(doc2);
            c3.setUnidad(u2);
            entityManager.persist(c3);

            entityManager.flush();
            return new Fixture(adminGlobal.getId(), u1.getUnidadId(), u2.getUnidadId());
        });

        assertNotNull(fixture);

        // 1. ADMIN_UNIDAD (U1) -> only sees c1 (usuarioAfectado != null && unidadId == U1), even if passing unidad2Id!
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("au1_audit_test", "pass", List.of()));
        Page<CambioResponseDTO> unitResult = cambioService.consultarHistorial(
                null, null, null, null, null, null, fixture.unidad2Id(), PageRequest.of(0, 20));
        assertEquals(1, unitResult.getTotalElements());
        assertEquals("domicilio", unitResult.getContent().get(0).getCampoAfectado());
        assertEquals(fixture.unidad1Id(), unitResult.getContent().get(0).getUnidadId());

        // 2. AUDITOR -> sees all 3 records ordered fechaHora DESC (c3, c2, c1)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("aud_audit_test", "pass", List.of()));
        Page<CambioResponseDTO> auditorResult = cambioService.consultarHistorial(
                null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null, null, null,
                PageRequest.of(0, 20));
        assertEquals(3, auditorResult.getTotalElements());
        assertEquals("BAJA", auditorResult.getContent().get(0).getTipoOperacion());
        assertEquals("ALTA", auditorResult.getContent().get(1).getTipoOperacion());
        assertEquals("MODIFICACION", auditorResult.getContent().get(2).getTipoOperacion());

        // 3. Combined filter by usuarioAfectado ("perez"), date range, tipoOperacion, usuarioId, categoria
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ag_audit_test", "pass", List.of()));
        Page<CambioResponseDTO> filtered = cambioService.consultarHistorial(
                "perez",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 10),
                TipoOperacion.MODIFICACION,
                fixture.adminGlobalId(),
                "Docente",
                null,
                PageRequest.of(0, 20));
        assertEquals(1, filtered.getTotalElements());
        assertEquals("Perez, Juan", filtered.getContent().get(0).getUsuarioAfectadoNombreCompleto());
        assertEquals("Docente", filtered.getContent().get(0).getCategoria());
    }
}

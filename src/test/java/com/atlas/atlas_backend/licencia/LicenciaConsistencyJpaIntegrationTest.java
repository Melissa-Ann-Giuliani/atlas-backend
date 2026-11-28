package com.atlas.atlas_backend.licencia;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.unidad.TipoUnidad;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.Permiso;
import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:licenciaconsistencydb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "atlas.licencias.sync-on-startup=false"
})
class LicenciaConsistencyJpaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LicenciaExpirationService licenciaExpirationService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    @WithMockUser(username = "admin_lic_consistency", roles = {"ADMIN_GLOBAL"})
    void allEndpointsAgreeOnActiveVsExpiredLicenciasAndScheduledJobTransitionsExpired() throws Exception {
        record FixtureIds(
                Integer docActiveLicId,
                Integer desActiveLicId,
                Integer cargoActiveLicId,
                Integer docExpired1Id,
                Integer desExpired1Id,
                Integer cargoExpired1Id,
                Integer docExpired2Id,
                Integer desExpired2Id,
                Integer cargoExpired2Id,
                Integer docFutureLicId,
                Integer desFutureLicId) {}

        LocalDate today = LocalDate.now();

        FixtureIds ids = Objects.requireNonNull(transactionTemplate.execute(status -> {
            Permiso permiso = new Permiso();
            permiso.setNombre("LIC_CONSISTENCY_READ");
            entityManager.persist(permiso);

            Rol rolAdmin = new Rol();
            rolAdmin.setNombre("Administrador Global");
            rolAdmin.setPermisos(Set.of(permiso));
            entityManager.persist(rolAdmin);

            Rol rolDocente = new Rol();
            rolDocente.setNombre("Docente");
            rolDocente.setPermisos(Set.of(permiso));
            entityManager.persist(rolDocente);

            Usuario admin = new Usuario();
            admin.setUsername("admin_lic_consistency");
            admin.setContrasenia("secret");
            admin.setCorreo("admin_lic_consistency@test.com");
            admin.setApellido("Admin");
            admin.setNombre("Global");
            admin.setActivo(true);
            admin.setRol(rolAdmin);
            entityManager.persist(admin);

            TipoUnidad tipoDepto = new TipoUnidad();
            tipoDepto.setNombre("Departamento");
            entityManager.persist(tipoDepto);

            Unidad uMat = new Unidad();
            uMat.setUnidadNombre("Matemática");
            uMat.setTipoUnidad(tipoDepto);
            entityManager.persist(uMat);

            Origen origenPlanta = new Origen();
            origenPlanta.setOrigenNombre("Planta");
            entityManager.persist(origenPlanta);

            Caracter carEfectivo = new Caracter();
            carEfectivo.setCaracterNombre("Efectivo");
            entityManager.persist(carEfectivo);

            Categoria catTitular = new Categoria();
            catTitular.setCategoriaNombre("Titular");
            entityManager.persist(catTitular);

            Dedicacion dedExclusiva = new Dedicacion();
            dedExclusiva.setDedicacionNombre("Exclusiva");
            dedExclusiva.setDedicacionHoras(40);
            entityManager.persist(dedExclusiva);

            MotivoLicencia motivo = new MotivoLicencia();
            motivo.setId(501);
            motivo.setNombre("Enfermedad");
            motivo.setDiasCorresp(30);
            entityManager.persist(motivo);

            // 1. Docente regularly Activo
            persistDocente(rolDocente, "cons_activo", 9001, 60000001L, 20600000001L,
                    "Activo", "Activo", "Asignado", 9101L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);

            // 2. Docente on ACTIVE Licencia (ends today -> still active)
            PersistedTriple activeLic = persistDocente(rolDocente, "cons_lic_activa", 9002, 60000002L, 20600000002L,
                    "Licencia", "Licencia", "Licencia", 9102L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistLicencia("LIC-ACT-1", today.minusDays(10), null, today, 10, motivo, activeLic.designacion());

            // 3. Docente with EXPIRED Licencia #1 (fechaFinEstim in the past, stale DB state = 'Licencia')
            PersistedTriple expLic1 = persistDocente(rolDocente, "cons_lic_exp1", 9003, 60000003L, 20600000003L,
                    "Licencia", "Licencia", "Licencia", 9103L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistLicencia("LIC-EXP-1", today.minusDays(30), null, today.minusDays(5), 25, motivo, expLic1.designacion());

            // 4. Docente with EXPIRED Licencia #2 (ended early: fechaFin in the past overrides future fechaFinEstim)
            PersistedTriple expLic2 = persistDocente(rolDocente, "cons_lic_exp2", 9004, 60000004L, 20600000004L,
                    "Licencia", "Licencia", "Licencia", 9104L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistLicencia("LIC-EXP-2", today.minusDays(20), today.minusDays(1), today.plusDays(20), 19, motivo, expLic2.designacion());

            // 5. Docente with FUTURE Licencia (starts in 5 days -> NOT active today)
            PersistedTriple futureLic = persistDocente(rolDocente, "cons_lic_future", 9005, 60000005L, 20600000005L,
                    "Licencia", "Licencia", "Licencia", 9105L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistLicencia("LIC-FUT-1", today.plusDays(5), null, today.plusDays(25), 20, motivo, futureLic.designacion());

            // 6. Inactivo docente with a Licencia row -> excluded from ALL 3 endpoints
            PersistedTriple inactivo = persistDocente(rolDocente, "cons_inactivo", 9006, 60000006L, 20600000006L,
                    "Inactivo", "Licencia", "Licencia", 9106L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistLicencia("LIC-INA-1", today.minusDays(5), null, today.plusDays(5), 10, motivo, inactivo.designacion());

            entityManager.flush();
            return new FixtureIds(
                    activeLic.docente().getId(), activeLic.designacion().getDesignacionId(), activeLic.cargo().getId(),
                    expLic1.docente().getId(), expLic1.designacion().getDesignacionId(), expLic1.cargo().getId(),
                    expLic2.docente().getId(), expLic2.designacion().getDesignacionId(), expLic2.cargo().getId(),
                    futureLic.docente().getId(), futureLic.designacion().getDesignacionId());
        }));

        // --- PART 1: BEFORE scheduled job runs (query-time consistency) ---
        assertAllEndpointsConsistent();

        // --- PART 2: Run scheduled expiration job ---
        int transitioned = licenciaExpirationService.sincronizarLicenciasVencidas(today);
        assertTrue(transitioned >= 4, "Expected expired designaciones and docentes to be transitioned");

        transactionTemplate.executeWithoutResult(status -> {
            entityManager.clear();
            // Active licencia stays Licencia
            assertEquals("Licencia", entityManager.find(Docente.class, ids.docActiveLicId()).getEstado());
            assertEquals("Licencia", entityManager.find(Designacion.class, ids.desActiveLicId()).getDesignacionEstadoActual());
            assertEquals("Licencia", entityManager.find(Cargo.class, ids.cargoActiveLicId()).getEstado());

            // Expired #1 transitioned to Activo / Asignado
            assertEquals("Activo", entityManager.find(Docente.class, ids.docExpired1Id()).getEstado());
            assertEquals("Activo", entityManager.find(Designacion.class, ids.desExpired1Id()).getDesignacionEstadoActual());
            assertEquals("Asignado", entityManager.find(Cargo.class, ids.cargoExpired1Id()).getEstado());

            // Expired #2 transitioned to Activo / Asignado
            assertEquals("Activo", entityManager.find(Docente.class, ids.docExpired2Id()).getEstado());
            assertEquals("Activo", entityManager.find(Designacion.class, ids.desExpired2Id()).getDesignacionEstadoActual());
            assertEquals("Asignado", entityManager.find(Cargo.class, ids.cargoExpired2Id()).getEstado());

            // Future licencia is NOT expired, so not reverted
            assertEquals("Licencia", entityManager.find(Docente.class, ids.docFutureLicId()).getEstado());
            assertEquals("Licencia", entityManager.find(Designacion.class, ids.desFutureLicId()).getDesignacionEstadoActual());
        });

        // --- PART 3: AFTER scheduled job runs (endpoints remain 100% consistent) ---
        assertAllEndpointsConsistent();
    }

    private void assertAllEndpointsConsistent() throws Exception {
        // 1. GET /api/inicio/mapa-docente
        mockMvc.perform(get("/api/inicio/mapa-docente").param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1));

        mockMvc.perform(get("/api/inicio/mapa-docente").param("estado", "Activo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(4));

        mockMvc.perform(get("/api/inicio/mapa-docente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(5));

        // 2. GET /api/docentes
        mockMvc.perform(get("/api/docentes").param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].legajo").value(9002))
                .andExpect(jsonPath("$.content[0].estado").value("Licencia"));

        mockMvc.perform(get("/api/docentes").param("estado", "Activo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].estado", Objects.requireNonNull(everyItem(is("Activo")))));

        // 3. GET /api/licencias
        mockMvc.perform(get("/api/licencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nroResolucion").value("LIC-ACT-1"));
    }

    private record PersistedTriple(Docente docente, Designacion designacion, Cargo cargo) {}

    private PersistedTriple persistDocente(
            Rol rolDocente,
            String username,
            Integer legajo,
            Long dni,
            Long cuil,
            String docenteEstado,
            String designacionEstado,
            String cargoEstado,
            Long cargoCodigo,
            Origen origen,
            Caracter caracter,
            Categoria categoria,
            Dedicacion dedicacion,
            Unidad unidad) {
        Docente doc = new Docente();
        doc.setUsername(username);
        doc.setContrasenia("secret");
        doc.setCorreo(username + "@test.com");
        doc.setApellido("Apellido");
        doc.setNombre("Nombre");
        doc.setActivo(true);
        doc.setRol(rolDocente);
        doc.setLegajo(legajo);
        doc.setTipoDni("DNI");
        doc.setDni(dni);
        doc.setCuil(cuil);
        doc.setEmailInstitucional(username + "@uni.edu");
        doc.setFechaIngreso(LocalDate.of(2020, 1, 1));
        doc.setEstado(docenteEstado);
        entityManager.persist(doc);

        Designacion des = new Designacion();
        des.setDesignacionNumeroResolucion("RES-" + legajo);
        des.setDesignacionFechaInicio(LocalDate.of(2026, 1, 1));
        des.setDesignacionFechaFin(LocalDate.of(2027, 12, 31));
        des.setDesignacionEstadoActual(designacionEstado);
        des.setUsuarioId(doc.getId());
        entityManager.persist(des);

        Cargo cargo = new Cargo();
        cargo.setCodigo(cargoCodigo);
        cargo.setHorasOcupadas(10);
        cargo.setFechaCreacion(LocalDate.of(2025, 1, 1));
        cargo.setOrigen(origen);
        cargo.setCaracter(caracter);
        cargo.setCategoria(categoria);
        cargo.setDedicacion(dedicacion);
        cargo.setDesignacion(des);
        cargo.setUnidad(unidad);
        cargo.setEstado(cargoEstado);
        entityManager.persist(cargo);

        return new PersistedTriple(doc, des, cargo);
    }

    private void persistLicencia(
            String nroResolucion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalDate fechaFinEstim,
            int cantidadDias,
            MotivoLicencia motivo,
            Designacion designacion) {
        Licencia l = new Licencia();
        l.setNroResolucion(nroResolucion);
        l.setFechaInicio(fechaInicio);
        l.setFechaFin(fechaFin);
        l.setFechaFinEstim(fechaFinEstim);
        l.setCantidadDias(cantidadDias);
        l.setMotivo(motivo);
        l.setDesignacion(designacion);
        entityManager.persist(l);
    }
}

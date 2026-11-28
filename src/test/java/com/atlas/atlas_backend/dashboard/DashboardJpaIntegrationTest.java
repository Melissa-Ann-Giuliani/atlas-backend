package com.atlas.atlas_backend.dashboard;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.licencia.Licencia;
import com.atlas.atlas_backend.licencia.MotivoLicencia;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboardtestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DashboardJpaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    @WithMockUser(username = "admin_map_test", roles = {"ADMINISTRADOR_GLOBAL"})
    void testMapaDocenteFiltersByOrigenEstadoAndMultiValues() throws Exception {
        record FixtureIds(
                Integer origenPlantaId,
                Integer origenInvId,
                Integer caracterEfectivoId,
                Integer caracterInterinoId,
                Integer categoriaTitularId,
                Integer dedicacionExclusivaId) {}

        FixtureIds ids = Objects.requireNonNull(transactionTemplate.execute(status -> {
            Permiso permiso = new Permiso();
            permiso.setNombre("DASH_READ");
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
            admin.setUsername("admin_map_test");
            admin.setContrasenia("secret");
            admin.setCorreo("admin_map@test.com");
            admin.setApellido("Admin");
            admin.setNombre("Global");
            admin.setActivo(true);
            admin.setRol(rolAdmin);
            entityManager.persist(admin);

            TipoUnidad tipoDepto = new TipoUnidad();
            tipoDepto.setNombre("Departamento");
            entityManager.persist(tipoDepto);

            TipoUnidad tipoCentro = new TipoUnidad();
            tipoCentro.setNombre("Centro");
            entityManager.persist(tipoCentro);

            Unidad uMat = new Unidad();
            uMat.setUnidadNombre("Matemática");
            uMat.setTipoUnidad(tipoDepto);
            entityManager.persist(uMat);

            Unidad uCoral = new Unidad();
            uCoral.setUnidadNombre("Creación Coral");
            uCoral.setTipoUnidad(tipoCentro);
            entityManager.persist(uCoral);

            Origen origenPlanta = new Origen();
            origenPlanta.setOrigenNombre("Planta");
            entityManager.persist(origenPlanta);

            Origen origenInv = new Origen();
            origenInv.setOrigenNombre("Investigación");
            entityManager.persist(origenInv);

            Caracter carEfectivo = new Caracter();
            carEfectivo.setCaracterNombre("Efectivo");
            entityManager.persist(carEfectivo);

            Caracter carInterino = new Caracter();
            carInterino.setCaracterNombre("Interino");
            entityManager.persist(carInterino);

            Categoria catTitular = new Categoria();
            catTitular.setCategoriaNombre("Titular");
            entityManager.persist(catTitular);

            Dedicacion dedExclusiva = new Dedicacion();
            dedExclusiva.setDedicacionNombre("Exclusiva");
            dedExclusiva.setDedicacionHoras(40);
            entityManager.persist(dedExclusiva);

            // Docente 1: Activo, Planta, Efectivo, Departamento Matemática
            persistDocenteWithCargo(rolDocente, "map_doc1", 7001, 50000001L, 20500000001L,
                    "Activo", "Activo", 8101L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);

            // Docente 2: Licencia (active date range), Investigación, Interino, Centro Creación Coral
            Designacion desLic = persistDocenteWithCargo(rolDocente, "map_doc2", 7002, 50000002L, 20500000002L,
                    "Licencia", "Licencia", 8102L, origenInv, carInterino, catTitular, dedExclusiva, uCoral);

            MotivoLicencia motivo = new MotivoLicencia();
            motivo.setId(901);
            motivo.setNombre("Enfermedad");
            motivo.setDiasCorresp(30);
            entityManager.persist(motivo);

            Licencia licenciaActiva = new Licencia();
            licenciaActiva.setNroResolucion("LIC-MAP-1");
            licenciaActiva.setFechaInicio(LocalDate.now().minusDays(5));
            licenciaActiva.setFechaFinEstim(LocalDate.now().plusDays(15));
            licenciaActiva.setCantidadDias(20);
            licenciaActiva.setMotivo(motivo);
            licenciaActiva.setDesignacion(desLic);
            entityManager.persist(licenciaActiva);

            // Docente 3: Inactivo (should never be counted, matching Listado)
            persistDocenteWithCargo(rolDocente, "map_doc3", 7003, 50000003L, 20500000003L,
                    "Inactivo", "Inactivo", 8103L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);

            entityManager.flush();
            return new FixtureIds(
                    origenPlanta.getOrigenId(),
                    origenInv.getOrigenId(),
                    carEfectivo.getCaracterId(),
                    carInterino.getCaracterId(),
                    catTitular.getCategoriaId(),
                    dedExclusiva.getDedicacionId());
        }));

        // 1. Omitted estado -> returns both Activo and Licencia (2), excludes Inactivo
        mockMvc.perform(get("/api/inicio/mapa-docente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2))
                .andExpect(jsonPath("$.departamentos[0].label").value("Matemática"))
                .andExpect(jsonPath("$.departamentos[0].value").value(1))
                .andExpect(jsonPath("$.centros[0].label").value("Creación Coral"))
                .andExpect(jsonPath("$.centros[0].value").value(1));

        // 2. estado=Activo -> only Activo (1)
        mockMvc.perform(get("/api/inicio/mapa-docente").param("estado", "Activo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1))
                .andExpect(jsonPath("$.departamentos[0].value").value(1))
                .andExpect(jsonPath("$.centros").isEmpty());

        // 3. estado=Licencia -> only Licencia (1)
        mockMvc.perform(get("/api/inicio/mapa-docente").param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1))
                .andExpect(jsonPath("$.centros[0].value").value(1))
                .andExpect(jsonPath("$.departamentos").isEmpty());

        // 4. Repeatable estado=Activo&estado=Licencia -> both (2)
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("estado", "Activo")
                        .param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2));

        // 5. Filter by single origenId -> Planta (1)
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("origenId", String.valueOf(ids.origenPlantaId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1))
                .andExpect(jsonPath("$.departamentos[0].label").value("Matemática"));

        // 6. Repeatable origenId -> Planta + Investigación (2)
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("origenId", String.valueOf(ids.origenPlantaId()))
                        .param("origenId", String.valueOf(ids.origenInvId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2));

        // 7. Repeatable caracterId + combined categoriaId & dedicacionId -> both (2)
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("caracterId", String.valueOf(ids.caracterEfectivoId()))
                        .param("caracterId", String.valueOf(ids.caracterInterinoId()))
                        .param("categoriaId", String.valueOf(ids.categoriaTitularId()))
                        .param("dedicacionId", String.valueOf(ids.dedicacionExclusivaId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2));

        // 8. Combined filters with no match -> origenId=Planta + estado=Licencia -> 0
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("origenId", String.valueOf(ids.origenPlantaId()))
                        .param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(0));
    }

    private Designacion persistDocenteWithCargo(
            Rol rolDocente,
            String username,
            Integer legajo,
            Long dni,
            Long cuil,
            String docenteEstado,
            String designacionEstado,
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
        des.setDesignacionFechaFin(LocalDate.of(2026, 12, 31));
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
        cargo.setEstado("Asignado");
        entityManager.persist(cargo);
        return des;
    }
}

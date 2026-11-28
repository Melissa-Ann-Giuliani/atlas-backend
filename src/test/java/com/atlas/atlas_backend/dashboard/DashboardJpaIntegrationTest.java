package com.atlas.atlas_backend.dashboard;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.licencia.Licencia;
import com.atlas.atlas_backend.licencia.MotivoLicencia;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.unidad.TipoUnidad;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.CargoAdminUnidad;
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
        // Non-ADMIN_UNIDAD should not receive materias/unidadNombre/tipoUnidadNombre
        mockMvc.perform(get("/api/inicio/mapa-docente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2))
                .andExpect(jsonPath("$.departamentos[0].label").value("Matemática"))
                .andExpect(jsonPath("$.departamentos[0].value").value(1))
                .andExpect(jsonPath("$.centros[0].label").value("Creación Coral"))
                .andExpect(jsonPath("$.centros[0].value").value(1))
                .andExpect(jsonPath("$.materias").doesNotExist())
                .andExpect(jsonPath("$.unidadNombre").doesNotExist())
                .andExpect(jsonPath("$.tipoUnidadNombre").doesNotExist());

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

    @Test
    @WithMockUser(username = "admin_unidad_mat", roles = {"ADMIN_UNIDAD"})
    void testMapaDocenteForAdminUnidadIncludesMateriasAndUnidadMetadata() throws Exception {
        record AdminUnidadFixtureIds(
                Integer origenPlantaId,
                Integer caracterEfectivoId,
                Integer caracterInterinoId,
                Integer categoriaTitularId,
                Integer dedicacionExclusivaId) {}

        AdminUnidadFixtureIds ids = Objects.requireNonNull(transactionTemplate.execute(status -> {
            Permiso permiso = new Permiso();
            permiso.setNombre("DASH_READ_AU");
            entityManager.persist(permiso);

            Rol rolAdminUnidad = new Rol();
            rolAdminUnidad.setNombre("ADMIN_UNIDAD");
            rolAdminUnidad.setPermisos(Set.of(permiso));
            entityManager.persist(rolAdminUnidad);

            Rol rolDocente = new Rol();
            rolDocente.setNombre("Docente_AU");
            rolDocente.setPermisos(Set.of(permiso));
            entityManager.persist(rolDocente);

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

            Unidad uOtra = new Unidad();
            uOtra.setUnidadNombre("Música");
            uOtra.setTipoUnidad(tipoCentro);
            entityManager.persist(uOtra);

            AdminUnidad adminUnidad = new AdminUnidad();
            adminUnidad.setUsername("admin_unidad_mat");
            adminUnidad.setContrasenia("secret");
            adminUnidad.setCorreo("admin_unidad_mat@test.com");
            adminUnidad.setApellido("Admin");
            adminUnidad.setNombre("Unidad");
            adminUnidad.setActivo(true);
            adminUnidad.setRol(rolAdminUnidad);
            adminUnidad.setCargoNombre(CargoAdminUnidad.DIRECTOR);
            adminUnidad.setUnidad(uMat);
            entityManager.persist(adminUnidad);

            Origen origenPlanta = new Origen();
            origenPlanta.setOrigenNombre("Planta_AU");
            entityManager.persist(origenPlanta);

            Caracter carEfectivo = new Caracter();
            carEfectivo.setCaracterNombre("Efectivo_AU");
            entityManager.persist(carEfectivo);

            Caracter carInterino = new Caracter();
            carInterino.setCaracterNombre("Interino_AU");
            entityManager.persist(carInterino);

            Categoria catTitular = new Categoria();
            catTitular.setCategoriaNombre("Titular_AU");
            entityManager.persist(catTitular);

            Dedicacion dedExclusiva = new Dedicacion();
            dedExclusiva.setDedicacionNombre("Exclusiva_AU");
            dedExclusiva.setDedicacionHoras(40);
            entityManager.persist(dedExclusiva);

            // Active materias in Matemática
            Materia mAnalisis = persistMateria("Análisis I", "Activa", uMat);
            Materia mCalculo = persistMateria("Cálculo I", "Activa", uMat);
            Materia mGeometria = persistMateria("Geometría", "Activa", uMat);
            // Inactive materia in Matemática (must be excluded)
            persistMateria("Topología Vieja", "Dada de baja", uMat);
            // Active materia in another unidad (must be excluded)
            Materia mArmonia = persistMateria("Armonía", "Activa", uOtra);

            // Docente 1: Activo, Efectivo, assigned twice to Análisis I (must count as 1 distinct docente)
            Cargo cargo1 = persistDocenteAndCargo(rolDocente, "au_doc1", 8001, 60000001L, 20600000001L,
                    "Activo", "Activo", 9101L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistFuncion("Teoría", cargo1, mAnalisis);
            persistFuncion("Práctica", cargo1, mAnalisis);

            // Docente 2: Licencia (active), Interino, assigned to Análisis I and Cálculo I
            Cargo cargo2 = persistDocenteAndCargo(rolDocente, "au_doc2", 8002, 60000002L, 20600000002L,
                    "Licencia", "Licencia", 9102L, origenPlanta, carInterino, catTitular, dedExclusiva, uMat);
            persistFuncion("Dictado", cargo2, mAnalisis);
            persistFuncion("Dictado", cargo2, mCalculo);

            MotivoLicencia motivo = new MotivoLicencia();
            motivo.setId(902);
            motivo.setNombre("Estudio");
            motivo.setDiasCorresp(30);
            entityManager.persist(motivo);

            Licencia lic = new Licencia();
            lic.setNroResolucion("LIC-AU-1");
            lic.setFechaInicio(LocalDate.now().minusDays(3));
            lic.setFechaFinEstim(LocalDate.now().plusDays(10));
            lic.setCantidadDias(13);
            lic.setMotivo(motivo);
            lic.setDesignacion(cargo2.getDesignacion());
            entityManager.persist(lic);

            // Docente 3: Inactivo in Matemática assigned to Geometría (must not be counted)
            Cargo cargo3 = persistDocenteAndCargo(rolDocente, "au_doc3", 8003, 60000003L, 20600000003L,
                    "Inactivo", "Inactivo", 9103L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uMat);
            persistFuncion("Dictado", cargo3, mGeometria);

            // Docente 4: Activo in Música assigned to Armonía (other unidad, must not be counted)
            Cargo cargo4 = persistDocenteAndCargo(rolDocente, "au_doc4", 8004, 60000004L, 20600000004L,
                    "Activo", "Activo", 9104L, origenPlanta, carEfectivo, catTitular, dedExclusiva, uOtra);
            persistFuncion("Dictado", cargo4, mArmonia);

            entityManager.flush();
            return new AdminUnidadFixtureIds(
                    origenPlanta.getOrigenId(),
                    carEfectivo.getCaracterId(),
                    carInterino.getCaracterId(),
                    catTitular.getCategoriaId(),
                    dedExclusiva.getDedicacionId());
        }));

        // 1. Unfiltered: totalDocentes=2; materias=[Análisis I:2, Cálculo I:1, Geometría:0]
        mockMvc.perform(get("/api/inicio/mapa-docente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(2))
                .andExpect(jsonPath("$.unidadNombre").value("Matemática"))
                .andExpect(jsonPath("$.tipoUnidadNombre").value("Departamento"))
                .andExpect(jsonPath("$.materias.length()").value(3))
                .andExpect(jsonPath("$.materias[0].label").value("Análisis I"))
                .andExpect(jsonPath("$.materias[0].value").value(2))
                .andExpect(jsonPath("$.materias[1].label").value("Cálculo I"))
                .andExpect(jsonPath("$.materias[1].value").value(1))
                .andExpect(jsonPath("$.materias[2].label").value("Geometría"))
                .andExpect(jsonPath("$.materias[2].value").value(0));

        // 2. Filtered by estado=Activo & caracterId=Efectivo -> totalDocentes=1; Análisis I:1, Cálculo I:0, Geometría:0
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("estado", "Activo")
                        .param("caracterId", String.valueOf(ids.caracterEfectivoId()))
                        .param("categoriaId", String.valueOf(ids.categoriaTitularId()))
                        .param("dedicacionId", String.valueOf(ids.dedicacionExclusivaId()))
                        .param("origenId", String.valueOf(ids.origenPlantaId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1))
                .andExpect(jsonPath("$.materias.length()").value(3))
                .andExpect(jsonPath("$.materias[0].label").value("Análisis I"))
                .andExpect(jsonPath("$.materias[0].value").value(1))
                .andExpect(jsonPath("$.materias[1].label").value("Cálculo I"))
                .andExpect(jsonPath("$.materias[1].value").value(0))
                .andExpect(jsonPath("$.materias[2].label").value("Geometría"))
                .andExpect(jsonPath("$.materias[2].value").value(0));

        // 3. Filtered by estado=Licencia -> totalDocentes=1; Análisis I:1, Cálculo I:1, Geometría:0
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("estado", "Licencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(1))
                .andExpect(jsonPath("$.materias.length()").value(3))
                .andExpect(jsonPath("$.materias[0].label").value("Análisis I"))
                .andExpect(jsonPath("$.materias[0].value").value(1))
                .andExpect(jsonPath("$.materias[1].label").value("Cálculo I"))
                .andExpect(jsonPath("$.materias[1].value").value(1))
                .andExpect(jsonPath("$.materias[2].label").value("Geometría"))
                .andExpect(jsonPath("$.materias[2].value").value(0));

        // 4. Filter with no matching docentes -> all 3 active materias still returned with value=0
        mockMvc.perform(get("/api/inicio/mapa-docente")
                        .param("estado", "Activo")
                        .param("caracterId", String.valueOf(ids.caracterInterinoId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocentes").value(0))
                .andExpect(jsonPath("$.materias.length()").value(3))
                .andExpect(jsonPath("$.materias[0].value").value(0))
                .andExpect(jsonPath("$.materias[1].value").value(0))
                .andExpect(jsonPath("$.materias[2].value").value(0));
    }

    private Materia persistMateria(String nombre, String estado, Unidad unidad) {
        Materia m = new Materia();
        m.setNombre(nombre);
        m.setHoras(6);
        m.setAnio("1");
        m.setDespliegue("Anual");
        m.setPlanEstudio(2020);
        m.setEstado(estado);
        m.setUnidad(unidad);
        entityManager.persist(m);
        return m;
    }

    private void persistFuncion(String nombre, Cargo cargo, Materia materia) {
        Funcion f = new Funcion();
        f.setNombre(nombre);
        f.setTipo("Docencia");
        f.setHoras(4);
        f.setCargo(cargo);
        f.setMateria(materia);
        entityManager.persist(f);
    }

    private Cargo persistDocenteAndCargo(
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
        return cargo;
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

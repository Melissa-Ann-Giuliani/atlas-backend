package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.funcion.AsignarFuncionesCargoRequest;
import com.atlas.atlas_backend.funcion.DesignacionCompletaResponseDTO;
import com.atlas.atlas_backend.funcion.FuncionItemRequest;
import com.atlas.atlas_backend.funcion.FuncionService;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.unidad.TipoUnidad;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.CargoAdminUnidad;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.Permiso;
import com.atlas.atlas_backend.usuario.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class DesignacionJpaIntegrationTest {

    @Autowired
    private DesignacionService designacionService;

    @Autowired
    private FuncionService funcionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void testRegistrarDesignacionYAsignarFunciones() {
        record FixtureIds(Integer docenteId, Integer cargoId, Integer materiaId, String adminUsername) {}

        FixtureIds ids = transactionTemplate.execute(status -> {
            Permiso p1 = new Permiso();
            p1.setNombre("READ_ALL");
            entityManager.persist(p1);

            Rol rolAdmin = new Rol();
            rolAdmin.setNombre("ADMIN_UNIDAD");
            rolAdmin.setPermisos(Set.of(p1));
            entityManager.persist(rolAdmin);

            Rol rolDocente = new Rol();
            rolDocente.setNombre("DOCENTE");
            rolDocente.setPermisos(Set.of(p1));
            entityManager.persist(rolDocente);

            TipoUnidad tipoUnidad = new TipoUnidad();
            tipoUnidad.setNombre("Facultad");
            entityManager.persist(tipoUnidad);

            Unidad unidad = new Unidad();
            unidad.setUnidadNombre("Ingenieria");
            unidad.setTipoUnidad(tipoUnidad);
            entityManager.persist(unidad);

            AdminUnidad admin = new AdminUnidad();
            admin.setUsername("admin_unidad_test");
            admin.setContrasenia("secret");
            admin.setCorreo("admin@test.com");
            admin.setApellido("Admin");
            admin.setNombre("Unit");
            admin.setActivo(true);
            admin.setRol(rolAdmin);
            admin.setCargoNombre(CargoAdminUnidad.values()[0]);
            admin.setUnidad(unidad);
            entityManager.persist(admin);

            Docente docente = new Docente();
            docente.setUsername("docente_test");
            docente.setContrasenia("secret");
            docente.setCorreo("docente@test.com");
            docente.setApellido("Perez");
            docente.setNombre("Juan");
            docente.setActivo(false);
            docente.setRol(rolDocente);
            docente.setLegajo(12345);
            docente.setTipoDni("DNI");
            docente.setDni(30123456L);
            docente.setCuil(20301234567L);
            docente.setEmailInstitucional("jperez@uni.edu");
            docente.setFechaIngreso(LocalDate.of(2020, 1, 1));
            docente.setEstado("Inactivo");
            entityManager.persist(docente);

            Origen origen = new Origen();
            origen.setOrigenNombre("Presupuesto");
            entityManager.persist(origen);

            Caracter caracter = new Caracter();
            caracter.setCaracterNombre("Regular");
            entityManager.persist(caracter);

            Categoria categoria = new Categoria();
            categoria.setCategoriaNombre("Titular");
            entityManager.persist(categoria);

            Dedicacion dedicacion = new Dedicacion();
            dedicacion.setDedicacionNombre("Exclusiva");
            dedicacion.setDedicacionHoras(40);
            entityManager.persist(dedicacion);

            Cargo cargo = new Cargo();
            cargo.setCodigo(1001L);
            cargo.setHorasOcupadas(0);
            cargo.setFechaCreacion(LocalDate.now());
            cargo.setOrigen(origen);
            cargo.setCaracter(caracter);
            cargo.setCategoria(categoria);
            cargo.setDedicacion(dedicacion);
            cargo.setUnidad(unidad);
            cargo.setEstado("Libre");
            entityManager.persist(cargo);

            Materia materia = new Materia();
            materia.setNombre("Algoritmos");
            materia.setHoras(10);
            materia.setAnio("1");
            materia.setDespliegue("Anual");
            materia.setPlanEstudio(2023);
            materia.setEstado("Activa");
            materia.setUnidad(unidad);
            entityManager.persist(materia);

            entityManager.flush();
            return new FixtureIds(docente.getId(), cargo.getId(), materia.getId(), admin.getUsername());
        });

        assertNotNull(ids);

        DesignacionCreateRequest desReq = new DesignacionCreateRequest();
        desReq.setDocenteId(ids.docenteId());
        desReq.setCargoId(ids.cargoId());
        desReq.setNroResolucion("RES-2026-001");
        desReq.setFechaInicio(LocalDate.of(2026, 3, 1));
        desReq.setFechaFin(LocalDate.of(2026, 12, 31));

        DesignacionResponseDTO desResp = designacionService.registrarDesignacion(desReq, ids.adminUsername());
        assertNotNull(desResp);
        assertNotNull(desResp.getId());

        AsignarFuncionesCargoRequest funcReq = new AsignarFuncionesCargoRequest();
        FuncionItemRequest item = new FuncionItemRequest();
        item.setNombre("Docencia");
        item.setTipo("Docencia");
        item.setHoras(10);
        item.setMateriaId(ids.materiaId());
        funcReq.setFunciones(List.of(item));

        DesignacionCompletaResponseDTO funcResp = funcionService.asignarFuncionesACargo(
                ids.cargoId(), funcReq, ids.adminUsername());
        assertNotNull(funcResp);
        assertEquals(1, funcResp.getFunciones().size());
    }
}

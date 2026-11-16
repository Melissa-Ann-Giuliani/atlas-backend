package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.Docente;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DesignacionPdfServiceTest {

    @Test
    void generarPdfDesignacion_ProducesValidPdfBytes() {
        DesignacionPdfService pdfService = new DesignacionPdfService();

        Designacion designacion = new Designacion();
        designacion.setDesignacionId(55);
        designacion.setDesignacionNumeroResolucion("RES-2026-100");
        designacion.setDesignacionFechaInicio(LocalDate.of(2026, 3, 1));
        designacion.setDesignacionFechaFin(LocalDate.of(2027, 2, 28));
        designacion.setDesignacionEstadoActual("Activo");

        Unidad unidad = new Unidad();
        unidad.setUnidadNombre("Facultad de Ingeniería");

        Dedicacion dedicacion = new Dedicacion();
        dedicacion.setDedicacionNombre("Semiexclusiva");
        dedicacion.setDedicacionHoras(20);

        Categoria categoria = new Categoria();
        categoria.setCategoriaNombre("Profesor Adjunto");

        Caracter caracter = new Caracter();
        caracter.setCaracterNombre("Ordinario");

        Cargo cargo = new Cargo();
        cargo.setCodigo(5014L);
        cargo.setHorasOcupadas(20);
        cargo.setUnidad(unidad);
        cargo.setDedicacion(dedicacion);
        cargo.setCategoria(categoria);
        cargo.setCaracter(caracter);

        Docente docente = new Docente();
        docente.setNombre("Ana");
        docente.setApellido("Gómez");
        docente.setDni(32111222L);
        docente.setCuil(27321112224L);
        docente.setEstado("Activo");

        Materia materia = new Materia();
        materia.setNombre("Algoritmos y Estructuras de Datos");

        Funcion funcion = new Funcion();
        funcion.setNombre("Docencia Frente a Curso");
        funcion.setTipo("Planta");
        funcion.setHoras(20);
        funcion.setMateria(materia);

        byte[] bytes = pdfService.generarPdfDesignacion(designacion, cargo, docente, List.of(funcion));

        assertNotNull(bytes);
        assertTrue(bytes.length > 100);
        String header = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header);
    }
}


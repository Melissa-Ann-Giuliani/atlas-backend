package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.unidad.Unidad;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "cargos")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cargo_id")
    private Integer id;

    @Column(name = "cargo_codigo", nullable = false)
    private Long codigo;

    @Column(name = "cargo_horas_ocupadas", nullable = false)
    private Integer horasOcupadas;

    @Column(name = "cargo_fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    // Relación recursiva 1: Predecesor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_predecesor_id")
    private Cargo cargoPredecesor;

    // Relación recursiva 2: Origen de división
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_origen_division")
    private Cargo cargoOrigenDivision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = false)
    private Origen origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caracter_id", nullable = false)
    private Caracter caracter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dedicacion_id", nullable = false)
    private Dedicacion dedicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designacion_id", nullable = false)
    private Designacion designacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;
}

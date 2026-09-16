package com.atlas.atlas_backend.funcion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.materia.Materia;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "funciones")
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funcion_id")
    private Integer id;

    @Column(name = "funcion_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "funcion_horas", nullable = false)
    private Integer horas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;
}

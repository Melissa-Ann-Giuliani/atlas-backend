package com.atlas.atlas_backend.materia;

import com.atlas.atlas_backend.unidad.Unidad;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "materias")
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "materia_id")
    private Integer id;

    @Column(name = "materia_nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "materia_horas", nullable = false)
    private Integer horas;

    @Column(name = "materia_anio", nullable = false, length = 10)
    private String anio;

    @Column(name = "materia_despliegue", nullable = false, length = 50)
    private String despliegue;

    @Column(name = "materia_plan_estudio", nullable = false)
    private Integer planEstudio;

    @Column(name = "materia_estado", nullable = false, length = 20)
    private String estado = "Activa";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;
}

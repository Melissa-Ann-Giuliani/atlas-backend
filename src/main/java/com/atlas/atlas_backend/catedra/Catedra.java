package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.usuario.Docente;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "catedras")
public class Catedra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catedra_id")
    private Integer id;

    @Column(name = "catedra_nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "catedra_tipologia", nullable = false)
    private Integer tipologia;

    @Column(name = "catedra_estado", nullable = false, length = 15)
    private String estado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catedra_director_id", nullable = false, unique = true)
    private Docente director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;
}

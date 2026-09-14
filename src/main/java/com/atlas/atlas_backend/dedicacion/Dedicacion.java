package com.atlas.atlas_backend.dedicacion;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dedicaciones")
public class Dedicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dedicacion_id")
    private Integer dedicacionId;

    @Column(name = "dedicacion_nombre", nullable = false, length = 50)
    private String dedicacionNombre;

    @Column(name = "dedicacion_horas")
    private Integer dedicacionHoras;
}

package com.atlas.atlas_backend.unidad;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "unidades")
public class Unidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unidad_id")
    private Integer unidadId;

    @Column(name = "unidad_nombre", nullable = false, length = 100)
    private String unidadNombre;

    @Column(name = "tipo_unidad_id", nullable = false)
    private Integer tipoUnidadId;
}

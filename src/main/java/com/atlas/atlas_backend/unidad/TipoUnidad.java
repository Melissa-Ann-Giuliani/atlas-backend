package com.atlas.atlas_backend.unidad;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_de_unidad")
public class TipoUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_unidad_id")
    private Integer id;

    @Column(name = "tipo_unidad_nombre", length = 20)
    private String nombre;
}

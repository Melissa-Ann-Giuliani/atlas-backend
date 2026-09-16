package com.atlas.atlas_backend.origen;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "origen")
public class Origen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "origen_id")
    private Integer origenId;

    @Column(name = "origen_nombre", nullable = false, length = 50)
    private String origenNombre;
}

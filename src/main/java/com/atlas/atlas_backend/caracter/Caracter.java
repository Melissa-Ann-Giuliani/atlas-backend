package com.atlas.atlas_backend.caracter;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "caracteres")
public class Caracter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "caracter_id")
    private Integer caracterId;

    @Column(name = "caracter_nombre", nullable = false, length = 100)
    private String caracterNombre;
}

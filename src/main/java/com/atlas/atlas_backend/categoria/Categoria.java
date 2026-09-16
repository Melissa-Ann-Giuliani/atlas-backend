package com.atlas.atlas_backend.categoria;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoria_id")
    private Integer categoriaId;

    @Column(name = "categoria_nombre", nullable = false, length = 40)
    private String categoriaNombre;
}

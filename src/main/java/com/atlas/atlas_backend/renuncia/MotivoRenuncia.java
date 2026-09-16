package com.atlas.atlas_backend.renuncia;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "motivos_renuncias")
public class MotivoRenuncia {

    @Id
    @Column(name = "motivo_r_id")
    private Integer id;

    @Column(name = "motivo_r_nombre", nullable = false, length = 50)
    private String nombre;
}

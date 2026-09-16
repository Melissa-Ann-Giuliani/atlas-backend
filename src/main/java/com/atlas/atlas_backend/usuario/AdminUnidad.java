package com.atlas.atlas_backend.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_de_unidad")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class AdminUnidad extends Usuario {

    @Column(name = "cargo_ad_nombre", nullable = false, length = 40)
    private String cargoNombre;

    @Column(name = "unidad_id", nullable = false)
    private Integer unidadId; // Mapped as simple ID for brevity, can map as ManyToOne Unidades later
}

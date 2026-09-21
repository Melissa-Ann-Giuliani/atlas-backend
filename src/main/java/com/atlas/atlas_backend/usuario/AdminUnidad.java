package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.unidad.Unidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_de_unidad")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class AdminUnidad extends Usuario {

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_ad_nombre", nullable = false, length = 40)
    private CargoAdminUnidad cargoNombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;
}

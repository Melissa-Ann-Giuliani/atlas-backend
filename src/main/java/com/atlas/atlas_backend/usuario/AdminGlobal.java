package com.atlas.atlas_backend.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_globales")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class AdminGlobal extends Usuario {

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_ag_nombre", length = 40, nullable = false)
    private CargoAdminGlobal cargoNombre;
}

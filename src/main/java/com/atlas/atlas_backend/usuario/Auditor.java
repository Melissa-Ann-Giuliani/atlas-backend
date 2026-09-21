package com.atlas.atlas_backend.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auditores")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class Auditor extends Usuario {

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_auditor_nombre", length = 50)
    private CargoAuditor cargoNombre;
}

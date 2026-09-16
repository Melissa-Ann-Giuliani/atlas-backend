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

    // The table only has usuario_id as PK, no extra fields.
}

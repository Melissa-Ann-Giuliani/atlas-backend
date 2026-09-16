package com.atlas.atlas_backend.licencia;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "motivos_licencias")
public class MotivoLicencia {

    @Id
    @Column(name = "motivo_l_id")
    private Integer id;

    @Column(name = "motivo_l_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "motivo_l_dias_corresp", nullable = false)
    private Integer diasCorresp;
}

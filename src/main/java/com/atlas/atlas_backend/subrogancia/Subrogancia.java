package com.atlas.atlas_backend.subrogancia;

import com.atlas.atlas_backend.cargo.Cargo;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "subrogancias")
public class Subrogancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subrogancia_id")
    private Integer id;

    @Column(name = "subrogancia_nro_resolucion", length = 20)
    private String nroResolucion;

    @Column(name = "subrogancia_fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "subrogancia_fecha_fin")
    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;
}

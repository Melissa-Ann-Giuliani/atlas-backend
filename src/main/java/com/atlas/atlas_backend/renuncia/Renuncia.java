package com.atlas.atlas_backend.renuncia;

import com.atlas.atlas_backend.designacion.Designacion;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "renuncias")
public class Renuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renuncia_id")
    private Integer id;

    @Column(name = "renuncia_nro_resolucion", nullable = false, length = 20)
    private String nroResolucion;

    @Column(name = "renuncia_fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "renuncia_fecha_efectiva", nullable = false)
    private LocalDate fechaEfectiva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoRenuncia motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designacion_id", nullable = false)
    private Designacion designacion;
}

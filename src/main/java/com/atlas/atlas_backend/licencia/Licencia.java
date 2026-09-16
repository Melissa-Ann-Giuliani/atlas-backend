package com.atlas.atlas_backend.licencia;

import com.atlas.atlas_backend.designacion.Designacion;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "licencias")
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "licencia_id")
    private Integer id;

    @Column(name = "licencia_nro_resolucion", length = 20)
    private String nroResolucion;

    @Column(name = "licencia_fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "licencia_fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "licencia_fecha_fin_estim")
    private LocalDate fechaFinEstim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoLicencia motivo;

    @Column(name = "licencia_cantidad_dias", nullable = false)
    private Integer cantidadDias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designacion_id", nullable = false)
    private Designacion designacion;
}

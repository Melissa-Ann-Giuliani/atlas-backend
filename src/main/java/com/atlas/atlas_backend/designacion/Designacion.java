package com.atlas.atlas_backend.designacion;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "designaciones")
public class Designacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "designacion_id")
    private Integer designacionId;

    @Column(name = "designacion_numero_resolucion", nullable = false)
    private Integer designacionNumeroResolucion;

    @Column(name = "designacion_fecha_inicio", nullable = false)
    private LocalDate designacionFechaInicio;

    @Column(name = "designacion_fecha_fin")
    private LocalDate designacionFechaFin;

    @Column(name = "designacion_estado_actual", nullable = false, length = 40)
    private String designacionEstadoActual;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;
}

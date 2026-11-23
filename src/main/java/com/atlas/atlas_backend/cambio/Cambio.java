package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cambios")
public class Cambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cambio_id")
    private Integer id;

    @Column(name = "cambio_tabla_afectada", nullable = false, length = 50)
    private String tablaAfectada;

    @Column(name = "cambio_campo_afectado", nullable = false, length = 50)
    private String campoAfectado;

    @Column(name = "cambio_valor_anterior", length = 255)
    private String valorAnterior;

    @Column(name = "cambio_valor_nuevo", length = 255)
    private String valorNuevo;

    @Column(name = "cambio_fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "cambio_tipo_operacion", nullable = false, length = 20)
    private TipoOperacion tipoOperacion = TipoOperacion.MODIFICACION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_afectado_id")
    private Usuario usuarioAfectado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private Unidad unidad;
}

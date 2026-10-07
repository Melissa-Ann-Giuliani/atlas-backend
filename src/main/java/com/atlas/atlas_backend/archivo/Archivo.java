package com.atlas.atlas_backend.archivo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "archivos")
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archivo_id")
    private Integer archivoId;

    @Column(name = "archivo_nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "archivo_nombre_encriptado", nullable = false, unique = true)
    private String nombreEncriptado;

    @Column(name = "archivo_tipo_tramite", nullable = false, length = 50)
    private String tipoTramite;

    @Column(name = "archivo_tramite_id", nullable = false)
    private Integer tramiteId;

    @Column(name = "archivo_fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "archivo_tamano", nullable = false)
    private Long tamano;

    @Column(name = "archivo_tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

}

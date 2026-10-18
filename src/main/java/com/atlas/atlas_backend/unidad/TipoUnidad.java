package com.atlas.atlas_backend.unidad;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_de_unidad")
public class TipoUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_unidad_id")
    private Integer id;

    @Column(name = "tipo_unidad_nombre", length = 20)
    private String nombre;

    public String getTipoUnidadNombre() {
        return this.nombre;
    }

    public void setTipoUnidadNombre(String tipoUnidadNombre) {
        this.nombre = tipoUnidadNombre;
    }

    public Integer getTipoUnidadId() {
        return this.id;
    }

    public void setTipoUnidadId(Integer id) {
        this.id = id;
    }
}

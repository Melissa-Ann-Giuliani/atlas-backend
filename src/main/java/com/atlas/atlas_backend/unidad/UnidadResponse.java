package com.atlas.atlas_backend.unidad;

import lombok.Data;

@Data
public class UnidadResponse {
    private Integer unidadId;
    private String unidadNombre;
    private Integer tipoUnidadId;
    private String tipoUnidadNombre;

    public UnidadResponse() {
    }

    public UnidadResponse(Integer unidadId, String unidadNombre, Integer tipoUnidadId, String tipoUnidadNombre) {
        this.unidadId = unidadId;
        this.unidadNombre = unidadNombre;
        this.tipoUnidadId = tipoUnidadId;
        this.tipoUnidadNombre = tipoUnidadNombre;
    }

    public static UnidadResponse fromEntity(Unidad unidad) {
        if (unidad == null) {
            return null;
        }
        Integer tipoId = unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getId() : null;
        String tipoNombre = unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getNombre() : null;
        return new UnidadResponse(unidad.getUnidadId(), unidad.getUnidadNombre(), tipoId, tipoNombre);
    }
}

package com.atlas.atlas_backend.designacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignacionVencimientosResumenDTO {

    private Long totalPorVencer;
    private Integer umbralDias;
    private Integer unidadId;
    private String unidadNombre;
    private Boolean tieneVencimientosCriticos;
    private Long totalCriticos;
}

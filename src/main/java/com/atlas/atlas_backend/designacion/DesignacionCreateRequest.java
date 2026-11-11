package com.atlas.atlas_backend.designacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DesignacionCreateRequest {

    public DesignacionCreateRequest() {
    }

    @NotNull(message = "El ID del docente es obligatorio")
    private Integer docenteId;

    private Integer cargoId;

    @NotBlank(message = "El número de resolución es obligatorio")
    private String nroResolucion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;
}


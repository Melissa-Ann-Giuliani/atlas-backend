package com.atlas.atlas_backend.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocenteCargoUpdateRequest {

    // Designation attributes
    @NotNull(message = "El número de resolución es obligatorio")
    private Integer nroResolucion;

    @NotNull(message = "La fecha de inicio de designación es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private String estadoDesignacion;

    // Cargo attributes
    @NotNull(message = "El ID de categoría es obligatorio")
    private Integer categoriaId;

    @NotNull(message = "El ID de dedicación es obligatorio")
    private Integer dedicacionId;

    @NotNull(message = "El ID de carácter es obligatorio")
    private Integer caracterId;

    @NotNull(message = "El ID de origen es obligatorio")
    private Integer origenId;
}

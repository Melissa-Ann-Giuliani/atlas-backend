package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.funcion.FuncionItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DesignacionRedesignarRequest {

    public DesignacionRedesignarRequest() {
    }

    @NotBlank(message = "El número de resolución es obligatorio")
    private String nroResolucion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private Integer cargoId;

    @Valid
    private List<FuncionItemRequest> funciones;
}

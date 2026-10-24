package com.atlas.atlas_backend.usuario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocenteFuncionUpdateRequest {

    @NotBlank(message = "El nombre de la función es obligatorio")
    private String nombre;

    @NotNull(message = "Las horas son obligatorias")
    @Min(value = 1, message = "Las horas deben ser al menos 1")
    private Integer horas;

    @NotNull(message = "La materia es obligatoria")
    private Integer materiaId;
}

package com.atlas.atlas_backend.funcion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FuncionItemRequest {

    @NotBlank(message = "El nombre de la función es obligatorio")
    @Size(max = 100, message = "El nombre de la función no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El tipo de la función es obligatorio")
    @Size(max = 50, message = "El tipo de la función no puede superar los 50 caracteres")
    private String tipo;

    @NotNull(message = "La cantidad de horas es obligatoria")
    @Min(value = 1, message = "La cantidad de horas debe ser al menos 1")
    private Integer horas;

    @NotNull(message = "El ID de la materia es obligatorio")
    private Integer materiaId;
}


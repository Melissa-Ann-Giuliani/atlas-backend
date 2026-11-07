package com.atlas.atlas_backend.catedra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatedraUpdateRequestDTO {

    @NotBlank(message = "El nombre de la cátedra es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @NotNull(message = "La tipología es obligatoria")
    @Min(value = 1, message = "La tipología debe ser al menos 1")
    @Max(value = 4, message = "La tipología no puede ser mayor a 4")
    private Integer tipologia;

    @NotNull(message = "El director de la cátedra es obligatorio")
    private Integer directorId;
}


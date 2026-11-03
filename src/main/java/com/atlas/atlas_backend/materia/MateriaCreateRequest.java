package com.atlas.atlas_backend.materia;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MateriaCreateRequest {

    public MateriaCreateRequest() {
    }

    @NotBlank(message = "El nombre de la materia es obligatorio")
    @Size(max = 50, message = "El nombre de la materia no puede exceder los 50 caracteres")
    private String nombre;

    @NotNull(message = "La cantidad de horas es obligatoria")
    @Min(value = 1, message = "Las horas semanales deben ser al menos 1")
    @Max(value = 40, message = "Las horas semanales no pueden superar 40")
    private Integer horas;

    @NotBlank(message = "El año académico es obligatorio")
    @Size(max = 10, message = "El formato de año académico no puede superar los 10 caracteres")
    private String anio;

    @NotBlank(message = "El despliegue es obligatorio")
    @Pattern(
        regexp = "^(Anual|Primer Semestre|Segundo Semestre)$",
        message = "El despliegue debe ser 'Anual', 'Primer Semestre' o 'Segundo Semestre'"
    )
    private String despliegue;

    @NotNull(message = "El ID de unidad es obligatorio")
    private Integer unidadId;

    private Integer dependenciaId;

    @NotNull(message = "El plan de estudio es obligatorio")
    @Min(value = 1900, message = "El plan de estudio debe ser un año válido (>= 1900)")
    @Max(value = 2100, message = "El plan de estudio debe ser un año válido (<= 2100)")
    private Integer planEstudio;
}

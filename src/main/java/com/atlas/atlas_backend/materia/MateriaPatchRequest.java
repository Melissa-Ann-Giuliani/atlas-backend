package com.atlas.atlas_backend.materia;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MateriaPatchRequest {

    public MateriaPatchRequest() {
    }

    @Size(max = 50, message = "El nombre de la materia no puede exceder los 50 caracteres")
    private String nombre;

    @Min(value = 1, message = "Las horas semanales deben ser al menos 1")
    @Max(value = 40, message = "Las horas semanales no pueden superar 40")
    private Integer horas;

    @Size(max = 10, message = "El formato de año académico no puede superar los 10 caracteres")
    private String anio;

    @Pattern(
        regexp = "^(Anual|Primer Semestre|Segundo Semestre)$",
        message = "El despliegue debe ser 'Anual', 'Primer Semestre' o 'Segundo Semestre'"
    )
    private String despliegue;
}

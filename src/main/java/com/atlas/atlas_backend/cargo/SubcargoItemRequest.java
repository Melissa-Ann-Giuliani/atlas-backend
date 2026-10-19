package com.atlas.atlas_backend.cargo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SubcargoItemRequest {

    public SubcargoItemRequest() {
    }

    @NotNull(message = "El código es obligatorio")
    private Long codigo;

    @NotNull(message = "La fecha de creación es obligatoria")
    private LocalDate fechaCreacion;

    @NotNull(message = "El ID de origen es obligatorio")
    private Integer origenId;

    @NotNull(message = "El ID de carácter es obligatorio")
    private Integer caracterId;

    @NotNull(message = "El ID de categoría es obligatorio")
    private Integer categoriaId;

    @NotNull(message = "El ID de dedicación es obligatorio")
    private Integer dedicacionId;

    @NotNull(message = "El ID de unidad es obligatorio")
    private Integer unidadId;

    private Long predecesorCodigo;
}

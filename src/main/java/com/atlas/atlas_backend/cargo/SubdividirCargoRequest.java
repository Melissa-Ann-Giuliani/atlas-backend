package com.atlas.atlas_backend.cargo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SubdividirCargoRequest {

    public SubdividirCargoRequest() {
    }

    @NotNull(message = "El código del cargo origen es obligatorio")
    private Long cargoOrigenCodigo;

    @NotNull(message = "La lista de subcargos es obligatoria")
    @NotEmpty(message = "Debe proporcionar al menos 2 subcargos")
    @Size(min = 2, max = 4, message = "La cantidad de subcargos debe ser entre 2 y 4")
    @Valid
    private List<SubcargoItemRequest> subcargos;
}

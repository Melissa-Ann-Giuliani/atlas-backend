package com.atlas.atlas_backend.designacion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DesignacionVincularCargoRequest {

    public DesignacionVincularCargoRequest() {
    }

    public DesignacionVincularCargoRequest(Integer cargoId) {
        this.cargoId = cargoId;
    }

    @NotNull(message = "El ID del cargo es obligatorio")
    private Integer cargoId;
}


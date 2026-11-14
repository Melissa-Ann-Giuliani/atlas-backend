package com.atlas.atlas_backend.funcion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AsignarFuncionesCargoRequest {

    @NotEmpty(message = "Debe enviar al menos una función")
    @Valid
    private List<FuncionItemRequest> funciones;
}


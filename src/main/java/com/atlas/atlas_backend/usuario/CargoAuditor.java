package com.atlas.atlas_backend.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CargoAuditor {
    @JsonProperty("Director")
    Director,
    @JsonProperty("Vicedirector")
    Vicedirector,
    @JsonProperty("Secretaria Academica")
    SecretariaAcademica
}

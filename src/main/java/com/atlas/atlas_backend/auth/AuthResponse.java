package com.atlas.atlas_backend.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String token;
    private String username;
    private String rol;
    private Integer unidadId;
    private String unidadNombre;
    private Integer tipoUnidadId;
    private String tipoUnidadNombre;

    public AuthResponse() {
    }

    public AuthResponse(String token) {
        this.token = token;
    }
}


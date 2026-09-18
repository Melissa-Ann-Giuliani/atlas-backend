package com.atlas.atlas_backend.auth;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String username;
    private String correo;
}

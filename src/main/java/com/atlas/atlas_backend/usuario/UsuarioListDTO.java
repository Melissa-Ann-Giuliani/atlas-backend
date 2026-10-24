package com.atlas.atlas_backend.usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioListDTO {
    private Integer id;
    private Integer usuarioId;
    private String username;
    private String nombre;
    private String apellido;
    private String correo;
    private String rol;
    private String dni;
    private String cargo;
    private String unidad;
    private Boolean activo;
}

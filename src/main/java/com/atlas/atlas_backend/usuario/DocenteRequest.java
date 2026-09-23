package com.atlas.atlas_backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DocenteRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El DNI es obligatorio")
    private Long dni;

    @NotNull(message = "El CUIL es obligatorio")
    private Long cuil;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNac;

    @NotBlank(message = "El correo electrónico institucional es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String emailInstitucional;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;

    private String domicilio;

    private Integer antigPrevia;

    private List<String> telefonos;
}

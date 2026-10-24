package com.atlas.atlas_backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DocenteProfileUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private String tipoDni;

    private Long dni;

    private Long cuil;

    @NotBlank(message = "El correo electrónico institucional es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String emailInstitucional;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;

    private String domicilio;

    private Integer antigPrevia;

    private String estado;

    private List<String> telefonos;
}

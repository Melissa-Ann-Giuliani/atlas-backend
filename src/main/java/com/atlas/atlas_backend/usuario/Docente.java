package com.atlas.atlas_backend.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "docentes")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class Docente extends Usuario {

    @Column(name = "docente_dni", nullable = false, unique = true)
    private Long dni;

    @Column(name = "docente_cuil", nullable = false, unique = true)
    private Long cuil;

    @Column(name = "docente_fecha_nac", nullable = false)
    private LocalDate fechaNac;

    @Column(name = "docente_email_institucional", length = 60, unique = true)
    private String emailInstitucional;

    @Column(name = "docente_fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "docente_domicilio", length = 100)
    private String domicilio;

    @Column(name = "docente_antig_previa")
    private Integer antigPrevia;
}

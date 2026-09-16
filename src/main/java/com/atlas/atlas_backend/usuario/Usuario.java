package com.atlas.atlas_backend.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Integer id;

    @Column(name = "usuario_username", nullable = false, length = 50)
    private String username;

    @Column(name = "usuario_contrasenia", nullable = false, length = 50)
    private String contrasenia;

    @Column(name = "usuario_correo", length = 50)
    private String correo;

    @Column(name = "usuario_apellido", nullable = false, length = 50)
    private String apellido;

    @Column(name = "usuario_nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "usuario_activo", nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
}

package com.atlas.atlas_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByUsernameAndCorreo(String username, String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM Usuario u
        JOIN u.rol r
        WHERE (:rol IS NULL OR UPPER(r.nombre) = UPPER(:rol)
               OR (:rol = 'ADMIN_GLOBAL' AND (UPPER(r.nombre) = 'ADMIN_GLOBAL' OR UPPER(r.nombre) = 'ADMINISTRADOR GLOBAL' OR UPPER(r.nombre) = 'ADMINISTRADOR_GLOBAL'))
               OR (:rol = 'ADMIN_UNIDAD' AND (UPPER(r.nombre) = 'ADMIN_UNIDAD' OR UPPER(r.nombre) = 'ADMINISTRADOR DE UNIDAD' OR UPPER(r.nombre) = 'ADMINISTRADOR_DE_UNIDAD'))
               OR (:rol = 'AUDITOR' AND (UPPER(r.nombre) = 'AUDITOR'))
               OR (:rol = 'DOCENTE' AND (UPPER(r.nombre) = 'DOCENTE')))
          AND (CAST(:search AS string) IS NULL OR (
               LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(u.correo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          ))
    """)
    org.springframework.data.domain.Page<Usuario> searchUsuarios(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("rol") String rol,
            org.springframework.data.domain.Pageable pageable
    );
}


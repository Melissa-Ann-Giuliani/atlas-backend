package com.atlas.atlas_backend.cambio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambioResponseDTO {

    private Integer id;
    private LocalDateTime fechaHora;
    private UsuarioResumenDTO usuarioResponsable;
    private String categoria;
    private String campoAfectado;
    private String valorAnterior;
    private String valorNuevo;
    private String tipoOperacion;
    private Integer usuarioAfectadoId;
    private String usuarioAfectadoNombreCompleto;
    private Integer unidadId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioResumenDTO {
        private Integer id;
        private String username;
        private String nombre;
        private String apellido;
    }
}

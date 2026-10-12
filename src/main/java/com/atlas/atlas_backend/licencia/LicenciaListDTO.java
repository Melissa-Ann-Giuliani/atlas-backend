package com.atlas.atlas_backend.licencia;

import java.time.LocalDate;

public interface LicenciaListDTO {
    Integer getId();
    String getNroResolucion();
    String getMotivoNombre();
    String getDocenteNombreCompleto();
    LocalDate getFechaInicio();
    LocalDate getFechaFin();
    Integer getDiasUsados();
    String getEstado();
}

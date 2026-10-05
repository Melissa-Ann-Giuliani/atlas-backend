package com.atlas.atlas_backend.cargo;

import java.time.LocalDate;

public interface CargoListDTO {
    Integer getId();
    Long getNumero();
    LocalDate getFechaCreacion();
    String getEstado();
    String getApellidoNombre();
    String getCategoria();
    String getDedicacion();
    String getCaracter();
}

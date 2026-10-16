package com.atlas.atlas_backend.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CargoAdminUnidad {
    DIRECTOR("Director"),
    VICEDIRECTOR("Vice Director"),
    SECRETARIA_ADMINISTRATIVA("Secretaría Administrativa");

    private final String label;

    CargoAdminUnidad(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static CargoAdminUnidad fromString(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        for (CargoAdminUnidad c : values()) {
            if (c.name().equalsIgnoreCase(clean) || c.label.equalsIgnoreCase(clean)) {
                return c;
            }
        }
        // Normalize common variations without accents or extra spaces
        String normalized = clean.toUpperCase()
                .replace("Í", "I")
                .replace(" ", "_");
        for (CargoAdminUnidad c : values()) {
            if (c.name().equalsIgnoreCase(normalized)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cargo de Administrador de Unidad inválido: " + value);
    }
}

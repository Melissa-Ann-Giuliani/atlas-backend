package com.atlas.atlas_backend.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CargoAuditor {
    DECANO("Decano"),
    VICEDECANO("Vicedecano"),
    SECRETARIA_ACADEMICA("Secretaría Académica");

    private final String label;

    CargoAuditor(String label) {
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
    public static CargoAuditor fromString(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        for (CargoAuditor c : values()) {
            if (c.name().equalsIgnoreCase(clean) || c.label.equalsIgnoreCase(clean)) {
                return c;
            }
        }
        // Normalize common variations without accents or extra spaces
        String normalized = clean.toUpperCase()
                .replace("Í", "I")
                .replace("É", "E")
                .replace(" ", "_");
        for (CargoAuditor c : values()) {
            if (c.name().equalsIgnoreCase(normalized)) {
                return c;
            }
        }
        // Also support variations without underscores
        for (CargoAuditor c : values()) {
            if (c.name().replace("_", "").equalsIgnoreCase(clean.replace(" ", ""))) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cargo de Auditor inválido: " + value);
    }
}

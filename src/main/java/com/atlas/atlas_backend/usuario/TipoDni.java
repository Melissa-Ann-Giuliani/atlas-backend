package com.atlas.atlas_backend.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoDni {
    DNI("DNI"),
    PASAPORTE("Pasaporte"),
    LC("LC"),
    LE("LE"),
    CI("Cédula de Identidad");

    private final String label;

    TipoDni(String label) {
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
    public static TipoDni fromString(String value) {
        if (value == null) {
            return null;
        }
        for (TipoDni tipo : TipoDni.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.label.equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo DNI no válido: " + value);
    }
}

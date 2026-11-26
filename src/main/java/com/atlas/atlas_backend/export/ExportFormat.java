package com.atlas.atlas_backend.export;

import org.springframework.http.MediaType;

public enum ExportFormat {
    PDF("pdf", MediaType.APPLICATION_PDF),
    XLSX("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private final String extension;
    private final MediaType mediaType;

    ExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return extension;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public static ExportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'formato' es obligatorio (valores permitidos: pdf, xlsx).");
        }
        return switch (value.trim().toLowerCase()) {
            case "pdf" -> PDF;
            case "xlsx", "xls", "excel" -> XLSX;
            default -> throw new IllegalArgumentException(
                    "Formato de exportación no soportado: '" + value + "'. Valores permitidos: pdf, xlsx.");
        };
    }
}

package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.usuario.Docente;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates official PDF documentation for completed docente designations.
 */
@Service
public class DesignacionPdfService {

    public byte[] generarPdfDesignacion(Designacion designacion, Cargo cargo, Docente docente,
            List<Funcion> funciones) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(33, 66, 99));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Constancia de Designación Docente", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            document.add(title);

            addSectionTitle(document, "Datos de la Designación", sectionFont);
            addKeyValue(document, "Nro. Resolución:", designacion.getDesignacionNumeroResolucion(), labelFont,
                    valueFont);
            addKeyValue(document, "Fecha de Inicio:", formatDate(designacion.getDesignacionFechaInicio()), labelFont,
                    valueFont);
            addKeyValue(document, "Fecha de Finalización:",
                    designacion.getDesignacionFechaFin() != null
                            ? formatDate(designacion.getDesignacionFechaFin())
                            : "Sin fecha de finalización definida",
                    labelFont, valueFont);
            addKeyValue(document, "Estado:", designacion.getDesignacionEstadoActual(), labelFont, valueFont);

            addSectionTitle(document, "Datos del Docente", sectionFont);
            if (docente != null) {
                addKeyValue(document, "Apellido y Nombre:",
                        safe(docente.getApellido()) + ", " + safe(docente.getNombre()), labelFont, valueFont);
                addKeyValue(document, "DNI:", docente.getDni() != null ? docente.getDni().toString() : "-", labelFont,
                        valueFont);
                addKeyValue(document, "CUIL:", docente.getCuil() != null ? docente.getCuil().toString() : "-",
                        labelFont, valueFont);
                addKeyValue(document, "Estado Docente:", safe(docente.getEstado()), labelFont, valueFont);
            } else {
                addKeyValue(document, "Docente:", "No especificado", labelFont, valueFont);
            }

            addSectionTitle(document, "Datos del Cargo", sectionFont);
            addKeyValue(document, "Código de Cargo:", cargo.getCodigo() != null ? cargo.getCodigo().toString() : "-",
                    labelFont, valueFont);
            addKeyValue(document, "Unidad Académica:",
                    cargo.getUnidad() != null ? safe(cargo.getUnidad().getUnidadNombre()) : "-", labelFont, valueFont);
            addKeyValue(document, "Categoría:",
                    cargo.getCategoria() != null ? safe(cargo.getCategoria().getCategoriaNombre()) : "-", labelFont,
                    valueFont);
            addKeyValue(document, "Carácter:",
                    cargo.getCaracter() != null ? safe(cargo.getCaracter().getCaracterNombre()) : "-", labelFont,
                    valueFont);
            String dedicacionNombre = cargo.getDedicacion() != null
                    ? safe(cargo.getDedicacion().getDedicacionNombre())
                    : "-";
            Integer dedicacionHoras = cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionHoras() : null;
            addKeyValue(document, "Dedicación:",
                    dedicacionHoras != null ? dedicacionNombre + " (" + dedicacionHoras + " hs)" : dedicacionNombre,
                    labelFont, valueFont);
            addKeyValue(document, "Horas Ocupadas:",
                    cargo.getHorasOcupadas() != null ? cargo.getHorasOcupadas() + " hs" : "0 hs", labelFont,
                    valueFont);

            addSectionTitle(document, "Funciones Asignadas", sectionFont);
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3.2f, 1.8f, 3.2f, 1.2f });
            table.setSpacingBefore(6);

            addHeaderCell(table, "Función", labelFont);
            addHeaderCell(table, "Tipo", labelFont);
            addHeaderCell(table, "Materia", labelFont);
            addHeaderCell(table, "Horas", labelFont);

            for (Funcion funcion : funciones) {
                table.addCell(new Phrase(safe(funcion.getNombre()), valueFont));
                table.addCell(new Phrase(safe(funcion.getTipo()), valueFont));
                String materiaNombre = funcion.getMateria() != null ? safe(funcion.getMateria().getNombre()) : "-";
                table.addCell(new Phrase(materiaNombre, valueFont));
                table.addCell(new Phrase(funcion.getHoras() != null ? funcion.getHoras().toString() : "0", valueFont));
            }
            document.add(table);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Error al generar el PDF de la designación", e);
        }
    }

    private void addSectionTitle(Document document, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private void addKeyValue(Document document, String label, String value, Font labelFont, Font valueFont)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + " ", labelFont));
        p.add(new Phrase(safe(value), valueFont));
        p.setSpacingAfter(2);
        document.add(p);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 236, 242));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "-";
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }
}


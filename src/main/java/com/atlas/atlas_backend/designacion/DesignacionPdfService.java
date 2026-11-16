package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.usuario.Docente;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Generates official PDF documentation for completed docente designations
 * matching the frontend resolution preview.
 */
@Service
public class DesignacionPdfService {

    private static final float MARGIN_LR = 62.36f; // ~22mm
    private static final float MARGIN_TB = 56.69f; // ~20mm
    private static final float BODY_FONT_SIZE = 13.5f;
    private static final float LINE_HEIGHT_MULTIPLIER = 1.65f;
    private static final float FIRST_LINE_INDENT = 24f;

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STAMP_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy",
            Locale.forLanguageTag("es-AR"));

    public byte[] generarPdfDesignacion(Designacion designacion, Cargo cargo, Docente docente,
            List<Funcion> funciones) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, MARGIN_LR, MARGIN_LR, MARGIN_TB, MARGIN_TB);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Font bodyFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, BODY_FONT_SIZE, Color.BLACK);
            Font bodyBoldFont = FontFactory.getFont(FontFactory.TIMES_BOLD, BODY_FONT_SIZE, Color.BLACK);
            Font headerItalicFont = FontFactory.getFont(FontFactory.TIMES_ITALIC, 12f, Color.BLACK);
            Font headerBoldFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 10.5f, Color.BLACK);
            Font stampFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 11f, Color.BLACK);

            addDocumentHeader(document, writer, designacion, headerItalicFont, headerBoldFont, stampFont);

            String nroResolucion = safe(designacion != null ? designacion.getDesignacionNumeroResolucion() : null);
            String categoria = cargo != null && cargo.getCategoria() != null
                    ? safe(cargo.getCategoria().getCategoriaNombre())
                    : "-";
            String dedicacion = cargo != null && cargo.getDedicacion() != null
                    ? safe(cargo.getDedicacion().getDedicacionNombre())
                    : "-";
            String horasDedicacion = cargo != null && cargo.getDedicacion() != null
                    && cargo.getDedicacion().getDedicacionHoras() != null
                            ? cargo.getDedicacion().getDedicacionHoras().toString()
                            : "0";
            String caracter = cargo != null && cargo.getCaracter() != null
                    ? safe(cargo.getCaracter().getCaracterNombre())
                    : "-";
            String cargoNumero = cargo != null && cargo.getCodigo() != null ? cargo.getCodigo().toString() : "-";
            String unidadNombre = cargo != null && cargo.getUnidad() != null
                    ? safe(cargo.getUnidad().getUnidadNombre())
                    : "-";
            String horasOcupadas = cargo != null && cargo.getHorasOcupadas() != null
                    ? cargo.getHorasOcupadas().toString()
                    : "0";

            String apellidoMayus = docente != null && docente.getApellido() != null
                    ? docente.getApellido().toUpperCase(Locale.ROOT)
                    : "-";
            String nombre = docente != null ? safe(docente.getNombre()) : "-";
            String legajo = docente != null && docente.getLegajo() != null ? docente.getLegajo().toString() : "-";

            // VISTO:
            addSectionHeading(document, "VISTO:", bodyBoldFont);
            Paragraph visto = createBodyParagraph();
            visto.add(new Chunk("Las actuaciones administrativas relativas a la designación dispuesta por Resolución N° ",
                    bodyFont));
            visto.add(new Chunk(nroResolucion, bodyBoldFont));
            visto.add(new Chunk(", para proveer UN (1) cargo de categoría ", bodyFont));
            visto.add(new Chunk(categoria, bodyBoldFont));
            visto.add(new Chunk(", dedicación ", bodyFont));
            visto.add(new Chunk(dedicacion + " (" + horasDedicacion + " hs)", bodyBoldFont));
            visto.add(new Chunk(", carácter ", bodyFont));
            visto.add(new Chunk(caracter, bodyBoldFont));
            visto.add(new Chunk(", Código N° ", bodyFont));
            visto.add(new Chunk(cargoNumero, bodyBoldFont));
            visto.add(new Chunk(", para cumplir funciones en ", bodyFont));
            visto.add(new Chunk(formatFuncionesList(funciones), bodyBoldFont));
            visto.add(new Chunk(", en el ámbito de la Unidad Académica ", bodyFont));
            visto.add(new Chunk(unidadNombre, bodyBoldFont));
            visto.add(new Chunk(", y", bodyFont));
            document.add(visto);

            // CONSIDERANDO:
            addSectionHeading(document, "CONSIDERANDO:", bodyBoldFont);
            Paragraph cons1 = createBodyParagraph();
            cons1.add(new Chunk(
                    "Que se han cumplido los procedimientos reglamentarios vigentes en la Universidad Nacional de San Juan para la cobertura del cargo docente mencionado;",
                    bodyFont));
            document.add(cons1);

            Paragraph cons2 = createBodyParagraph();
            cons2.add(new Chunk("Que corresponde designar al/a la docente ", bodyFont));
            cons2.add(new Chunk(apellidoMayus + ", " + nombre, bodyBoldFont));
            cons2.add(new Chunk(" (Legajo N° ", bodyFont));
            cons2.add(new Chunk(legajo, bodyBoldFont));
            cons2.add(new Chunk("), a partir del ", bodyFont));
            String fechaInicio = designacion != null && designacion.getDesignacionFechaInicio() != null
                    ? designacion.getDesignacionFechaInicio().format(DD_MM_YYYY)
                    : "-";
            cons2.add(new Chunk(fechaInicio, bodyBoldFont));
            if (designacion != null && designacion.getDesignacionFechaFin() != null) {
                cons2.add(new Chunk(" y hasta el ", bodyFont));
                cons2.add(new Chunk(designacion.getDesignacionFechaFin().format(DD_MM_YYYY), bodyBoldFont));
            } else {
                cons2.add(new Chunk(", con carácter indeterminado", bodyFont));
            }
            cons2.add(new Chunk(", asignándole una carga horaria total de ", bodyFont));
            cons2.add(new Chunk(horasOcupadas + " horas semanales", bodyBoldFont));
            cons2.add(new Chunk(";", bodyFont));
            document.add(cons2);

            // POR ELLO,
            addSectionHeading(document, "POR ELLO,", bodyBoldFont);

            // LA DECANA...
            Paragraph decana = new Paragraph("LA DECANA DE LA FACULTAD DE FILOSOFÍA, HUMANIDADES Y ARTES",
                    bodyBoldFont);
            decana.setAlignment(Element.ALIGN_CENTER);
            decana.setLeading(0f, LINE_HEIGHT_MULTIPLIER);
            decana.setSpacingBefore(6f);
            decana.setSpacingAfter(8f);
            document.add(decana);

            // RESUELVE:
            addSectionHeading(document, "RESUELVE:", bodyBoldFont);

            // ARTÍCULO 1°.-
            Paragraph art1 = createBodyParagraph();
            art1.add(new Chunk("ARTÍCULO 1°.- ", bodyBoldFont));
            art1.add(new Chunk("Designar a ", bodyFont));
            art1.add(new Chunk(apellidoMayus + ", " + nombre, bodyBoldFont));
            art1.add(new Chunk(" (Legajo N° ", bodyFont));
            art1.add(new Chunk(legajo, bodyBoldFont));
            art1.add(new Chunk(") en el Cargo N° ", bodyFont));
            art1.add(new Chunk(cargoNumero, bodyBoldFont));
            art1.add(new Chunk(", Categoría ", bodyFont));
            art1.add(new Chunk(categoria, bodyBoldFont));
            art1.add(new Chunk(", Carácter ", bodyFont));
            art1.add(new Chunk(caracter, bodyBoldFont));
            art1.add(new Chunk(", Dedicación ", bodyFont));
            art1.add(new Chunk(dedicacion + " (" + horasDedicacion + " hs)", bodyBoldFont));
            art1.add(new Chunk(", dependiente de la Unidad ", bodyFont));
            art1.add(new Chunk(unidadNombre, bodyBoldFont));
            art1.add(new Chunk(", bajo Resolución N° ", bodyFont));
            art1.add(new Chunk(nroResolucion, bodyBoldFont));
            art1.add(new Chunk(", pasando a revestir en estado ", bodyFont));
            art1.add(new Chunk("ACTIVO", bodyBoldFont));
            art1.add(new Chunk(".", bodyFont));
            document.add(art1);

            // ARTÍCULO 2°.-
            Paragraph art2 = createBodyParagraph();
            art2.add(new Chunk("ARTÍCULO 2°.- ", bodyBoldFont));
            art2.add(new Chunk("Asignar al cargo mencionado las siguientes funciones académicas:", bodyFont));
            document.add(art2);

            if (funciones != null) {
                for (int i = 0; i < funciones.size(); i++) {
                    Funcion f = funciones.get(i);
                    String fNombre = safe(f.getNombre());
                    String fTipo = safe(f.getTipo());
                    String fHoras = f.getHoras() != null ? f.getHoras().toString() : "0";
                    String mNombre = f.getMateria() != null ? safe(f.getMateria().getNombre()) : "-";

                    Paragraph item = new Paragraph();
                    item.setAlignment(Element.ALIGN_JUSTIFIED);
                    item.setLeading(0f, LINE_HEIGHT_MULTIPLIER);
                    item.setIndentationLeft(FIRST_LINE_INDENT);
                    item.setSpacingAfter(4f);
                    item.add(new Chunk("• Función N°" + (i + 1) + ": " + fNombre + " — Tipo: " + fTipo
                            + " — Carga horaria: " + fHoras + " hs semanales — Materia: " + mNombre + ".", bodyFont));
                    document.add(item);
                }
            }

            // ARTÍCULO 3°.-
            Paragraph art3 = createBodyParagraph();
            art3.add(new Chunk("ARTÍCULO 3°.- ", bodyBoldFont));
            art3.add(new Chunk("Regístrese, comuníquese, notifíquese al/a la interesado/a y archívese.", bodyFont));
            document.add(art3);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Error al generar el PDF de la designación", e);
        }
    }

    private void addDocumentHeader(Document document, PdfWriter writer, Designacion designacion,
            Font italicFont, Font boldFont, Font stampFont) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[] { 62f, 38f });
        headerTable.setSpacingAfter(20f);

        // Left block: centered emblem + Universidad + Facultad
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);

        Image emblem = createUnsjEmblem(writer);
        if (emblem != null) {
            emblem.setAlignment(Element.ALIGN_CENTER);
            leftCell.addElement(emblem);
        }

        Paragraph univ = new Paragraph("Universidad Nacional de San Juan", italicFont);
        univ.setAlignment(Element.ALIGN_CENTER);
        univ.setSpacingBefore(4f);
        leftCell.addElement(univ);

        Paragraph fac = new Paragraph("FACULTAD DE FILOSOFÍA, HUMANIDADES Y ARTES", boldFont);
        fac.setAlignment(Element.ALIGN_CENTER);
        fac.setSpacingBefore(2f);
        leftCell.addElement(fac);

        headerTable.addCell(leftCell);

        // Right block: bottom-aligned stamp date
        LocalDate fecha = designacion != null && designacion.getDesignacionFechaInicio() != null
                ? designacion.getDesignacionFechaInicio()
                : LocalDate.now();
        String formattedDate = fecha.format(STAMP_DATE).replace(".", "").toUpperCase(Locale.ROOT);
        PdfPCell rightCell = new PdfPCell(new Phrase("SAN JUAN, " + formattedDate, stampFont));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        rightCell.setPaddingBottom(2f);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private Image createUnsjEmblem(PdfWriter writer) {
        try {
            float size = 44f;
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate template = cb.createTemplate(size, size);
            float cx = size / 2f;
            float cy = size / 2f;
            float outerR = size / 2f - 2f;
            float innerR = outerR - 4f;

            template.setLineWidth(1.2f);
            template.setColorStroke(Color.BLACK);
            template.circle(cx, cy, outerR);
            template.stroke();

            template.setLineWidth(0.7f);
            template.circle(cx, cy, innerR);
            template.stroke();

            BaseFont bf = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            template.beginText();
            template.setFontAndSize(bf, 9.5f);
            template.showTextAligned(PdfContentByte.ALIGN_CENTER, "UNSJ", cx, cy - 3.5f, 0);
            template.endText();

            return Image.getInstance(template);
        } catch (Exception e) {
            return null;
        }
    }

    private void addSectionHeading(Document document, String text, Font boldFont) throws DocumentException {
        Paragraph p = new Paragraph(text, boldFont);
        p.setLeading(0f, LINE_HEIGHT_MULTIPLIER);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private Paragraph createBodyParagraph() {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setFirstLineIndent(FIRST_LINE_INDENT);
        p.setLeading(0f, LINE_HEIGHT_MULTIPLIER);
        p.setSpacingAfter(6f);
        return p;
    }

    private String formatFuncionesList(List<Funcion> funciones) {
        if (funciones == null || funciones.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < funciones.size(); i++) {
            Funcion f = funciones.get(i);
            String materiaNombre = f.getMateria() != null ? safe(f.getMateria().getNombre()) : "-";
            String funcionNombre = safe(f.getNombre());
            if (i > 0) {
                sb.append(i == funciones.size() - 1 ? " y " : ", ");
            }
            sb.append("“").append(materiaNombre).append("” (").append(funcionNombre).append(")");
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }
}

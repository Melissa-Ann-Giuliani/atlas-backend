package com.atlas.atlas_backend.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter FILE_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Admin green palette (--admin-primary-900, 800, 100, 50)
    private static final Color ADMIN_TITLE_COLOR = new Color(26, 46, 28); // #1A2E1C
    private static final Color ADMIN_HEADER_BG = new Color(42, 75, 47); // #2A4B2F
    private static final Color ADMIN_ZEBRA_BG = new Color(239, 246, 240); // #EFF6F0
    private static final Color ADMIN_BORDER_COLOR = new Color(209, 229, 212); // #D1E5D4

    public ResponseEntity<byte[]> buildExportResponse(
            String baseFilename,
            String title,
            List<String> headers,
            List<List<String>> rows,
            String formato) {
        return buildExportResponse(baseFilename, title, headers, rows, formato, List.of());
    }

    public ResponseEntity<byte[]> buildExportResponse(
            String baseFilename,
            String title,
            List<String> headers,
            List<List<String>> rows,
            String formato,
            List<String> appliedFilters) {

        List<String> cleanFilters = sanitizeFilters(appliedFilters);
        ExportFormat exportFormat = ExportFormat.fromString(formato);
        byte[] content = switch (exportFormat) {
            case PDF -> exportToPdf(title, headers, rows, cleanFilters);
            case XLSX -> exportToExcel(title, headers, rows, cleanFilters);
        };

        String filename = baseFilename + "_" + LocalDateTime.now().format(FILE_DATETIME_FMT) + "."
                + exportFormat.getExtension();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(exportFormat.getMediaType());
        httpHeaders.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        httpHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(content);
    }

    public byte[] exportToExcel(String title, List<String> headers, List<List<String>> rows) {
        return exportToExcel(title, headers, rows, List.of());
    }

    public byte[] exportToExcel(String title, List<String> headers, List<List<String>> rows,
            List<String> appliedFilters) {
        List<String> cleanFilters = sanitizeFilters(appliedFilters);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String sheetName = sanitizeSheetName(title);
            Sheet sheet = workbook.createSheet(sheetName);

            // Title style
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(new XSSFColor(ADMIN_TITLE_COLOR, null));
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Subtitle style
            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 9);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(subtitleFont);

            // Header style
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(ADMIN_HEADER_BG, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorders(headerStyle);

            // Data styles (normal + zebra)
            XSSFCellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorders(dataStyle);

            XSSFCellStyle zebraStyle = workbook.createCellStyle();
            zebraStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            zebraStyle.setFillForegroundColor(new XSSFColor(ADMIN_ZEBRA_BG, null));
            zebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyThinBorders(zebraStyle);

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);

            // Row 1: Metadata
            Row metaRow = sheet.createRow(1);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue("Generado el " + LocalDateTime.now().format(DISPLAY_DATETIME_FMT)
                    + " | Total de registros: " + rows.size());
            metaCell.setCellStyle(subtitleStyle);

            // Optional Row 2: Applied filters card banner
            int headerRowIdx = 3;
            if (!cleanFilters.isEmpty()) {
                XSSFFont filterFont = workbook.createFont();
                filterFont.setBold(true);
                filterFont.setFontHeightInPoints((short) 10);
                filterFont.setColor(new XSSFColor(ADMIN_HEADER_BG, null));
                XSSFCellStyle filterStyle = workbook.createCellStyle();
                filterStyle.setFont(filterFont);
                filterStyle.setFillForegroundColor(new XSSFColor(ADMIN_ZEBRA_BG, null));
                filterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                filterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                applyThinBorders(filterStyle);

                Row filterRow = sheet.createRow(2);
                filterRow.setHeightInPoints(22);
                int lastCol = Math.max(0, headers.size() - 1);
                for (int col = 0; col <= lastCol; col++) {
                    Cell cell = filterRow.createCell(col);
                    if (col == 0) {
                        cell.setCellValue("FILTROS APLICADOS:  " + String.join("  •  ", cleanFilters));
                    }
                    cell.setCellStyle(filterStyle);
                }
                if (lastCol > 0) {
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, lastCol));
                }
                headerRowIdx = 4;
            }

            // Headers
            Row headerRow = sheet.createRow(headerRowIdx);
            headerRow.setHeightInPoints(22);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(rowIdx++);
                List<String> rowData = rows.get(i);
                CellStyle style = (i % 2 == 1) ? zebraStyle : dataStyle;
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = row.createCell(col);
                    String val = (col < rowData.size() && rowData.get(col) != null) ? rowData.get(col) : "";
                    cell.setCellValue(val);
                    cell.setCellStyle(style);
                }
            }

            // Auto-size columns with a cap
            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col) + 512;
                sheet.setColumnWidth(col, Math.min(width, 14000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }

    public byte[] exportToPdf(String title, List<String> headers, List<List<String>> rows) {
        return exportToPdf(title, headers, rows, List.of());
    }

    public byte[] exportToPdf(String title, List<String> headers, List<List<String>> rows,
            List<String> appliedFilters) {
        List<String> cleanFilters = sanitizeFilters(appliedFilters);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ADMIN_TITLE_COLOR);
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setSpacingAfter(4f);
            document.add(titleParagraph);

            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Paragraph metaParagraph = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(DISPLAY_DATETIME_FMT)
                            + "  |  Total de registros: " + rows.size(),
                    metaFont);
            metaParagraph.setSpacingAfter(cleanFilters.isEmpty() ? 12f : 4f);
            document.add(metaParagraph);

            if (!cleanFilters.isEmpty()) {
                PdfPTable filterCard = new PdfPTable(1);
                filterCard.setWidthPercentage(100f);
                filterCard.setSpacingBefore(4f);
                filterCard.setSpacingAfter(12f);

                Font filterLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, ADMIN_TITLE_COLOR);
                Font filterValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, ADMIN_HEADER_BG);
                Paragraph filterParagraph = new Paragraph();
                filterParagraph.add(new Phrase("FILTROS APLICADOS:  ", filterLabelFont));
                filterParagraph.add(new Phrase(String.join("   •   ", cleanFilters), filterValueFont));

                PdfPCell cardCell = new PdfPCell(filterParagraph);
                cardCell.setBackgroundColor(ADMIN_ZEBRA_BG);
                cardCell.setPaddingTop(7f);
                cardCell.setPaddingBottom(7f);
                cardCell.setPaddingLeft(10f);
                cardCell.setPaddingRight(10f);
                cardCell.setBorderColorTop(ADMIN_BORDER_COLOR);
                cardCell.setBorderColorRight(ADMIN_BORDER_COLOR);
                cardCell.setBorderColorBottom(ADMIN_BORDER_COLOR);
                cardCell.setBorderColorLeft(ADMIN_HEADER_BG);
                cardCell.setBorderWidthTop(0.75f);
                cardCell.setBorderWidthRight(0.75f);
                cardCell.setBorderWidthBottom(0.75f);
                cardCell.setBorderWidthLeft(3.5f);

                filterCard.addCell(cardCell);
                document.add(filterCard);
            }

            if (headers.isEmpty()) {
                document.close();
                return out.toByteArray();
            }

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100f);
            table.setHeaderRows(1);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(ADMIN_HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6f);
                cell.setBorderColor(ADMIN_BORDER_COLOR);
                table.addCell(cell);
            }

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Color zebraBg = ADMIN_ZEBRA_BG;
            Color borderColor = ADMIN_BORDER_COLOR;

            if (rows.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No se encontraron registros.", dataFont));
                emptyCell.setColspan(headers.size());
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10f);
                emptyCell.setBorderColor(borderColor);
                table.addCell(emptyCell);
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    List<String> rowData = rows.get(i);
                    Color rowBg = (i % 2 == 1) ? zebraBg : Color.WHITE;
                    for (int col = 0; col < headers.size(); col++) {
                        String val = (col < rowData.size() && rowData.get(col) != null) ? rowData.get(col) : "";
                        PdfPCell cell = new PdfPCell(new Phrase(val, dataFont));
                        cell.setBackgroundColor(rowBg);
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        cell.setPadding(5f);
                        cell.setBorderColor(borderColor);
                        table.addCell(cell);
                    }
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el archivo PDF", e);
        }
    }

    private void applyThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        short grey = IndexedColors.GREY_25_PERCENT.getIndex();
        style.setTopBorderColor(grey);
        style.setBottomBorderColor(grey);
        style.setLeftBorderColor(grey);
        style.setRightBorderColor(grey);
    }

    private List<String> sanitizeFilters(List<String> appliedFilters) {
        if (appliedFilters == null || appliedFilters.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String f : appliedFilters) {
            if (f != null && !f.isBlank()) {
                sanitized.add(f.trim());
            }
        }
        return sanitized;
    }

    private String sanitizeSheetName(String title) {
        if (title == null || title.isBlank()) {
            return "Listado";
        }
        String sanitized = title.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}

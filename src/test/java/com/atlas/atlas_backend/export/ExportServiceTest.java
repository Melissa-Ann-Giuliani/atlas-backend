package com.atlas.atlas_backend.export;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    @Test
    void exportFormatFromStringParsesSupportedFormats() {
        assertEquals(ExportFormat.PDF, ExportFormat.fromString("pdf"));
        assertEquals(ExportFormat.PDF, ExportFormat.fromString("PDF"));
        assertEquals(ExportFormat.XLSX, ExportFormat.fromString("xlsx"));
        assertEquals(ExportFormat.XLSX, ExportFormat.fromString("xls"));
        assertEquals(ExportFormat.XLSX, ExportFormat.fromString("excel"));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString("csv"));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString(null));
    }

    @Test
    void exportToExcelProducesValidXlsxWithHeadersAndRows() throws Exception {
        List<String> headers = List.of("Legajo", "Apellido", "Nombre");
        List<List<String>> rows = List.of(
                List.of("1001", "García", "Ana"),
                List.of("1002", "Pérez", "Juan")
        );

        byte[] bytes = exportService.exportToExcel("Listado de Docentes", headers, rows);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(1, wb.getNumberOfSheets());
            var sheet = wb.getSheetAt(0);
            assertEquals("Listado de Docentes", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Legajo", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Apellido", sheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("Nombre", sheet.getRow(3).getCell(2).getStringCellValue());
            assertEquals("1001", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("García", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("Juan", sheet.getRow(5).getCell(2).getStringCellValue());
        }
    }

    @Test
    void exportToPdfProducesValidPdfDocument() {
        List<String> headers = List.of("Número", "Estado", "Categoría");
        List<List<String>> rows = List.of(
                List.of("123", "ACTIVO", "Titular")
        );

        byte[] bytes = exportService.exportToPdf("Listado de Cargos", headers, rows);
        assertNotNull(bytes);
        assertTrue(bytes.length > 100);
        String header = new String(bytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header);
    }

    @Test
    void exportToPdfHandlesEmptyRows() {
        byte[] bytes = exportService.exportToPdf("Listado Vacío", List.of("Col 1", "Col 2"), List.of());
        assertNotNull(bytes);
        assertEquals("%PDF-", new String(bytes, 0, 5, StandardCharsets.US_ASCII));
    }

    @Test
    void buildExportResponseSetsHeadersForPdfAndXlsx() {
        List<String> headers = List.of("Col1");
        List<List<String>> rows = List.of(List.of("Val1"));

        ResponseEntity<byte[]> pdfResp = exportService.buildExportResponse(
                "listado_docentes", "Listado de Docentes", headers, rows, "pdf");
        assertEquals(HttpStatus.OK, pdfResp.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, pdfResp.getHeaders().getContentType());
        assertNotNull(pdfResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(pdfResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("listado_docentes_"));
        assertTrue(pdfResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).endsWith(".pdf\""));
        assertEquals(HttpHeaders.CONTENT_DISPOSITION,
                pdfResp.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));

        ResponseEntity<byte[]> xlsxResp = exportService.buildExportResponse(
                "listado_docentes", "Listado de Docentes", headers, rows, "xlsx");
        assertEquals(HttpStatus.OK, xlsxResp.getStatusCode());
        assertEquals(ExportFormat.XLSX.getMediaType(), xlsxResp.getHeaders().getContentType());
        assertTrue(xlsxResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).endsWith(".xlsx\""));
    }
}

package com.example.excelcreator;

import com.example.excelcreator.controller.ExportController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExportTests {

    @Autowired
    private ExportController exportController;

    @Test
    void testExcelExport() throws Exception {
        ResponseEntity<byte[]> response = exportController.exportExcel();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);

        // Optional: write to file to inspect manually
        try (FileOutputStream fos = new FileOutputStream("test_output.xlsx")) {
            fos.write(response.getBody());
        }
    }

    @Test
    void testPdfExport() throws Exception {
        ResponseEntity<byte[]> response = exportController.exportPdf();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);

        try (FileOutputStream fos = new FileOutputStream("test_output.pdf")) {
            fos.write(response.getBody());
        }
    }
}

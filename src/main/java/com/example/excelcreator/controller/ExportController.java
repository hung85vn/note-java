package com.example.excelcreator.controller;

import com.example.excelcreator.dto.OrderDetailDTO;
import com.example.excelcreator.dto.OrderExportDTO;
import com.example.excelcreator.service.BaseExportService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final BaseExportService exportService;

    public ExportController(BaseExportService exportService) {
        this.exportService = exportService;
    }

    private List<OrderExportDTO> getMockData() {
        List<OrderExportDTO> orders = new ArrayList<>();

        // Order 1
        List<OrderDetailDTO> details1 = new ArrayList<>();
        details1.add(new OrderDetailDTO("Laptop", 1, 1500.0));
        details1.add(new OrderDetailDTO("Mouse", 2, 25.0));
        orders.add(new OrderExportDTO("ORD-001", "John Doe", "2023-10-27", details1));

        // Order 2
        List<OrderDetailDTO> details2 = new ArrayList<>();
        details2.add(new OrderDetailDTO("Monitor", 2, 300.0));
        details2.add(new OrderDetailDTO("Keyboard", 1, 100.0));
        orders.add(new OrderExportDTO("ORD-002", "Jane Smith", "2023-10-28", details2));

        return orders;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            // Ensure template exists for demo purposes
            ensureTemplateExists();

            String templatePath = new File("template.xlsx").getAbsolutePath();
            byte[] content = exportService.exportExcel(getMockData(), templatePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] content = exportService.exportPdf(getMockData());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper to create a dummy template if not present
    private void ensureTemplateExists() {
        File f = new File("template.xlsx");
        if (!f.exists()) {
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(f)) {
                org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Order Template");

                // Create some static headers
                org.apache.poi.ss.usermodel.Row r = sheet.createRow(0);
                r.createCell(0).setCellValue("INVOICE");

                org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(1);
                r1.createCell(0).setCellValue("Order ID:"); // A2
                // B2 will be filled
                r1.createCell(3).setCellValue("Date:"); // D2
                // E2 will be filled

                org.apache.poi.ss.usermodel.Row r2 = sheet.createRow(2);
                r2.createCell(0).setCellValue("Customer:"); // A3
                // B3 will be filled

                // Table Header at Row 5 (index 4)
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(4);
                header.createCell(0).setCellValue("Product");
                header.createCell(1).setCellValue("Qty");
                header.createCell(2).setCellValue("Price");
                header.createCell(3).setCellValue("Total");

                wb.write(fos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

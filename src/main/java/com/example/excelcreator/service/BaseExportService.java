package com.example.excelcreator.service;

import com.example.excelcreator.annotation.ExcelCell;
import com.example.excelcreator.annotation.ExcelTable;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BaseExportService {

    // --- EXCEL LOGIC ---

    public <T> byte[] exportExcel(List<T> objects, String templatePath) throws Exception {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("List of objects is empty");
        }

        try (InputStream is = new FileInputStream(templatePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet templateSheet = workbook.getSheetAt(0);
            String templateSheetName = templateSheet.getSheetName();

            // First, clone the template sheet n-1 times if needed.
            // We must clone BEFORE we fill the first sheet to ensure we clone a clean template.
            int numberOfObjects = objects.size();
            for (int i = 1; i < numberOfObjects; i++) {
                Sheet cloned = workbook.cloneSheet(0);
                workbook.setSheetName(workbook.getSheetIndex(cloned), templateSheetName + " (" + (i + 1) + ")");
            }

            // Now loop through all sheets (which are now clean) and fill them
            for (int i = 0; i < numberOfObjects; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                fillSheet(sheet, objects.get(i));
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private <T> void fillSheet(Sheet sheet, T obj) throws Exception {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(obj);

            if (field.isAnnotationPresent(ExcelCell.class)) {
                ExcelCell annotation = field.getAnnotation(ExcelCell.class);
                String cellRef = annotation.value();
                setCellValue(sheet, cellRef, value);
            } else if (field.isAnnotationPresent(ExcelTable.class)) {
                ExcelTable annotation = field.getAnnotation(ExcelTable.class);
                if (value instanceof List<?>) {
                    fillTable(sheet, (List<?>) value, annotation);
                }
            }
        }
    }

    private void setCellValue(Sheet sheet, String cellRef, Object value) {
        CellReference ref = new CellReference(cellRef);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) {
            row = sheet.createRow(ref.getRow());
        }
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) {
            cell = row.createCell(ref.getCol());
        }

        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        } else {
            cell.setCellValue("");
        }
    }

    private void fillTable(Sheet sheet, List<?> list, ExcelTable annotation) throws Exception {
        int startRow = annotation.startRow();
        String[] mapping = annotation.columnMapping();

        // Shift rows if necessary to make space?
        // For simplicity in this demo, we assume the space is empty or we overwrite.
        // In a real robust engine, we might need sheet.shiftRows().

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            int currentRowIndex = startRow + i;
            Row row = sheet.getRow(currentRowIndex);
            if (row == null) {
                row = sheet.createRow(currentRowIndex);
            }

            for (String map : mapping) {
                // map format "fieldName:Column" e.g. "productName:A"
                String[] parts = map.split(":");
                String fieldName = parts[0].trim();
                String colLetter = parts[1].trim();

                Object val = getFieldValueOrGetter(item, fieldName);

                int colIndex = CellReference.convertColStringToIndex(colLetter);
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    cell = row.createCell(colIndex);
                }

                if (val != null) {
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else {
                        cell.setCellValue(val.toString());
                    }
                }
            }
        }
    }

    // --- PDF LOGIC ---

    public <T> byte[] exportPdf(List<T> objects) throws Exception {
        // Since we don't have a visual template engine for PDF in this demo (like JasperReports),
        // we will generate a PDF based on the Data.
        // The prompt says "copySheet so each PDF page corresponds to 1 object".

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, bos);

        document.open();

        for (int i = 0; i < objects.size(); i++) {
            T obj = objects.get(i);
            if (i > 0) {
                document.newPage();
            }
            generatePdfPage(document, obj);
        }

        document.close();
        return bos.toByteArray();
    }

    private <T> void generatePdfPage(Document document, T obj) throws Exception {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        // Simple dump of @ExcelCell annotated fields
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ExcelCell.class)) {
                Object value = field.get(obj);
                String label = field.getName(); // Use field name as label
                document.add(new Paragraph(label + ": " + (value != null ? value.toString() : "")));
            }
        }

        document.add(new Paragraph("\nDetails:\n"));

        // Table for @ExcelTable
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ExcelTable.class)) {
                Object value = field.get(obj);
                if (value instanceof List<?>) {
                    ExcelTable tableIdx = field.getAnnotation(ExcelTable.class);
                    String[] mapping = tableIdx.columnMapping();

                    PdfPTable table = new PdfPTable(mapping.length);
                    // Headers
                    for (String map : mapping) {
                        table.addCell(map.split(":")[0]);
                    }

                    List<?> list = (List<?>) value;
                    for (Object item : list) {
                        for (String map : mapping) {
                            String fieldName = map.split(":")[0];
                            Object val = getFieldValueOrGetter(item, fieldName);
                            table.addCell(val != null ? val.toString() : "");
                        }
                    }
                    document.add(table);
                }
            }
        }
    }

    /**
     * Tries to get value from Field, or if missing, from Getter method.
     */
    private Object getFieldValueOrGetter(Object item, String fieldName) {
        Class<?> clazz = item.getClass();

        // 1. Try Field
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(item);
        } catch (NoSuchFieldException e) {
            // Field not found, try getter
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // 2. Try Getter (getFieldName or isFieldName)
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            return clazz.getMethod("get" + capitalized).invoke(item);
        } catch (Exception e) {
            // try 'is'
        }

        try {
            return clazz.getMethod("is" + capitalized).invoke(item);
        } catch (Exception e) {
            // Getter not found either
        }

        return null;
    }
}

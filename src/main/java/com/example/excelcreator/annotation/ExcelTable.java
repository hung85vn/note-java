package com.example.excelcreator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelTable {
    /**
     * The row index (0-based) where the table data starts.
     */
    int startRow();

    /**
     * Mapping from Field Name to Column Letter (e.g., "productName:A, quantity:B").
     * If empty, it might assume order or require mapping elsewhere.
     * Simplest for this demo: Array of strings "field:Column".
     */
    String[] columnMapping();
}

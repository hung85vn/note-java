package com.example.excelcreator.dto;

import com.example.excelcreator.annotation.ExcelCell;
import com.example.excelcreator.annotation.ExcelTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExportDTO {

    @ExcelCell("B2") // Cell B2 in Excel
    private String orderId;

    @ExcelCell("B3") // Cell B3 in Excel
    private String customerName;

    @ExcelCell("E2") // Cell E2
    private String orderDate;

    // Start filling from row 6 (index 5)
    // Mapping: fieldName : Excel Column Letter
    @ExcelTable(startRow = 5, columnMapping = {
            "productName:A",
            "quantity:B",
            "unitPrice:C",
            "totalPrice:D"
    })
    private List<OrderDetailDTO> details;
}

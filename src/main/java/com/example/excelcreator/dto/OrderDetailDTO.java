package com.example.excelcreator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {
    private String productName;
    private int quantity;
    private double unitPrice;

    public double getTotalPrice() {
        return quantity * unitPrice;
    }
}

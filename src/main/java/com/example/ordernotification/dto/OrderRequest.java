package com.example.ordernotification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for POST /api/orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String customerId;
    private String customerEmail;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
}

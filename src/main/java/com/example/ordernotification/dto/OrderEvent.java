package com.example.ordernotification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OrderEvent is the message payload published to Kafka.
 * All three consumers receive this same object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String orderId;
    private String customerId;
    private String customerEmail;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;

    public enum OrderStatus {
        PLACED, CONFIRMED, CANCELLED
    }

    // Factory method for convenience
    public static OrderEvent create(String customerId, String customerEmail,
                                   String productId, String productName,
                                   int quantity, BigDecimal totalAmount) {
        return OrderEvent.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId(customerId)
                .customerEmail(customerEmail)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .orderTime(LocalDateTime.now())
                .build();
    }
}

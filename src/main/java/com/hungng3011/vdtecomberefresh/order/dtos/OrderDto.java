package com.hungng3011.vdtecomberefresh.order.dtos;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String id;
    private String userId; // Matches Keycloak user ID
    private OrderStatus status;
    private String address;
    private String phone;
    private String note;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus; // Changed from String to enum
    private String paymentId; // For payment gateway transaction ID
    private BigDecimal totalPrice; // Changed from String to BigDecimal
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> items; // List of order items, assuming OrderItem is another entity class
}

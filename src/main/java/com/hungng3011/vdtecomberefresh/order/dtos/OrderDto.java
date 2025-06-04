package com.hungng3011.vdtecomberefresh.order.dtos;

import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String paymentStatus;
    private String paymentId; // For payment gateway transaction ID
    private String totalPrice; // Total price of the order
    private String createdAt; // ISO 8601 format
    private String updatedAt; // ISO 8601 format
    private List<OrderItemDto> items; // List of order items, assuming OrderItem is another entity class
}

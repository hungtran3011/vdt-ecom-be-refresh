package com.hungng3011.vdtecomberefresh.order.entities;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;
    
    private String userId; // Matches Keycloak user ID
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private String address;
    private String phone;
    private String note;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // Changed from String to enum
    
    private String paymentId; // For payment gateway transaction ID
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalPrice; // Changed from String to BigDecimal
    
    private LocalDateTime createdAt; // Changed from String to LocalDateTime
    private LocalDateTime updatedAt; // Changed from String to LocalDateTime

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items; // List of order items, assuming OrderItem is another entity class
}

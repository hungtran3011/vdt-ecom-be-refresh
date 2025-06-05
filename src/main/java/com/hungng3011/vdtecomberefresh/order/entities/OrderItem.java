package com.hungng3011.vdtecomberefresh.order.entities;

import jakarta.persistence.*; // Import necessary annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // Specifies the foreign key column in the order_item table
    private Order order; // Replaced orderId with a direct reference to Order

    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    @Column(precision = 19, scale = 4)
    private BigDecimal price;
    @Column(precision = 19, scale = 4)
    private BigDecimal totalPrice;
}
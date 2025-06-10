package com.hungng3011.vdtecomberefresh.order.entities;

import com.hungng3011.vdtecomberefresh.product.entities.Product;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // Foreign key to Product
    private Product product; // Direct reference to Product instead of storing product details

    private Integer quantity;
    @Column(precision = 19, scale = 4)
    private BigDecimal price; // Price at time of order (historical price)
    @Column(precision = 19, scale = 4)
    private BigDecimal totalPrice; // quantity * price
}
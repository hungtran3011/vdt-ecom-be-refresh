package com.hungng3011.vdtecomberefresh.product.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_dynamic_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDynamicValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "field_id", nullable = false)
    private CategoryDynamicField field;

    @Column(nullable = false)
    private String value; // Stores actual value for a product attribute
}



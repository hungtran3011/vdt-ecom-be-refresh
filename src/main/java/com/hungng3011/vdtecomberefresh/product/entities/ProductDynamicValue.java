package com.hungng3011.vdtecomberefresh.product.entities;

import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_dynamic_values", indexes = {
        @Index(name = "idx_product_dynamic_value_product_id", columnList = "product_id"),
        @Index(name = "idx_product_dynamic_value_field_id", columnList = "field_id"),
        @Index(name = "idx_product_dynamic_value_value", columnList = "value")
})
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



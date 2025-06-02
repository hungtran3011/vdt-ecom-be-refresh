package com.hungng3011.vdtecomberefresh.product.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "variations", indexes = {
        @Index(name = "idx_variation_product_id", columnList = "product_id"),
        @Index(name = "idx_variation_type", columnList = "type"),
        @Index(name = "idx_variation_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Variation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String type; // e.g., "color", "RAM", "storage"

    @Column(nullable = false)
    private String name; // e.g., "Blue", "16GB"

    @Column(nullable = false)
    private BigDecimal additionalPrice;

    @OneToMany(mappedBy = "variation", cascade = CascadeType.ALL)
    private List<VariationDynamicValue> dynamicValues;
}


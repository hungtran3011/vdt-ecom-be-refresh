package com.hungng3011.vdtecomberefresh.product.entities;

import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "variation_dynamic_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariationDynamicValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variation_id", nullable = false)
    private Variation variation;

    @ManyToOne
    @JoinColumn(name = "field_id", nullable = false)
    private CategoryDynamicField field;

    @Column(nullable = false)
    private String value; // Stores actual value (e.g., "#FF0000" for color)
}

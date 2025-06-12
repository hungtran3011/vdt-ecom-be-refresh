package com.hungng3011.vdtecomberefresh.cart.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for adding items to cart with stock validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    
    @NotNull(message = "Product ID is required")
    @Min(value = 1, message = "Product ID must be positive")
    private Long productId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;
    
    /**
     * List of variation IDs for product variations
     * Optional - only needed for products with variations
     */
    private List<Long> variationIds;
    
    /**
     * Stock SKU for specific stock item selection
     * Optional - system will auto-select if not provided
     */
    private String stockSku;
}

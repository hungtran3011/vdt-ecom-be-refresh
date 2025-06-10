package com.hungng3011.vdtecomberefresh.stock.dtos;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for available variation combinations response
 * Provides all information frontend needs to display available options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableVariationDto {
    private Long stockId;
    private String sku;
    private Long productId;
    private String productName;
    private List<VariationDto> variations;
    private Integer availableQuantity;
    private StockStatus status;
    private boolean isAvailable; // Computed field: quantity > 0 && status IN (IN_STOCK, LOW_STOCK)
}

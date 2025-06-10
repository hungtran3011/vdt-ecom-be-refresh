package com.hungng3011.vdtecomberefresh.stock.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for variation combination requests from frontend
 * Used when frontend needs to check/validate specific product+variation combinations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariationCombinationDto {
    private Long productId;
    private List<Long> variationIds;
    private Integer quantity; // Optional - used for validation requests
}

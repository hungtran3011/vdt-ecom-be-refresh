package com.hungng3011.vdtecomberefresh.stock.dtos;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
    private Long id;
    private String sku;
    private Long productId;
    private String productName;
    private List<VariationDto> variations = new ArrayList<>();
    private Integer quantity;
    private Integer lowStockThreshold;
    private StockStatus status;
    private LocalDateTime updatedAt;
}

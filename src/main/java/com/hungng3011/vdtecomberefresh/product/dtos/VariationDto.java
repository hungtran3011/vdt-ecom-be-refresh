package com.hungng3011.vdtecomberefresh.product.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariationDto {
    private Long id;
    private Long productId;
    private String type;
    private String name;
    private BigDecimal additionalPrice;
    private List<VariationDynamicValueDto> dynamicValues;
}

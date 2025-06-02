package com.hungng3011.vdtecomberefresh.product.dtos;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariationDynamicValueDto {
    private Long id;
    private Long variationId;
    private CategoryDynamicFieldDto field;
    private String value;
}


package com.hungng3011.vdtecomberefresh.product.dtos;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDynamicValueDto {
    private Long id;
    private Long productId;
    private CategoryDynamicFieldDto field;
    private String value;
}


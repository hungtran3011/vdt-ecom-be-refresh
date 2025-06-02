package com.hungng3011.vdtecomberefresh.product.dtos;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDto;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private List<String> images;
    private CategoryDto category;
    private Long categoryId;
    private List<ProductDynamicValueDto> dynamicValues;
    private List<VariationDto> variations;
}

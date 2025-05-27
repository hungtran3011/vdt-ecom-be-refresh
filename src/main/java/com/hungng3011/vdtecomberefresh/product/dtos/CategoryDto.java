package com.hungng3011.vdtecomberefresh.product.dtos;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private List<CategoryDynamicFieldDto> dynamicFields;
}


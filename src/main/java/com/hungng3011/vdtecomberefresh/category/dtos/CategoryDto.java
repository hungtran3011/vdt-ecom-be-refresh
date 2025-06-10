package com.hungng3011.vdtecomberefresh.category.dtos;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private List<CategoryDynamicFieldDto> dynamicFields;
    private String imageUrl;
    
    // Product count - calculated field, ignored by entities
    private Long productCount = 0L;
}


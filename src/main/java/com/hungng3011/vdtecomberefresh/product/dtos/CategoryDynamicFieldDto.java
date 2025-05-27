package com.hungng3011.vdtecomberefresh.product.dtos;

import com.hungng3011.vdtecomberefresh.product.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.product.enums.FieldType;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDynamicFieldDto {
    private Long id;
    private Long categoryId;
    private String fieldName;
    private FieldType fieldType;
    private AppliesTo appliesTo;
}


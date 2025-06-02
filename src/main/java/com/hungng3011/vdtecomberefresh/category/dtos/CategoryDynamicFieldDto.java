package com.hungng3011.vdtecomberefresh.category.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hungng3011.vdtecomberefresh.category.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.category.enums.FieldType;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDynamicFieldDto {
    private Long id;
    @JsonIgnore
    private Long categoryId;
    private String fieldName;
    private FieldType fieldType;
    private AppliesTo appliesTo;
    private boolean required;
}


package com.hungng3011.vdtecomberefresh.product.dtos;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariationDynamicValueDto {
    private Long id;
    private Long variationId;
    private Long fieldId;
    private String value;
}


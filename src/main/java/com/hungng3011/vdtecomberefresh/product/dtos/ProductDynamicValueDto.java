package com.hungng3011.vdtecomberefresh.product.dtos;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDynamicValueDto {
    private Long id;
    private Long productId;
    private Long fieldId;
    private String value;
}


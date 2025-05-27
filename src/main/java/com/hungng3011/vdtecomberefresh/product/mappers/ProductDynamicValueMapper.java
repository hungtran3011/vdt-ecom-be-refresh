package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.ProductDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.entities.ProductDynamicValue;
import org.mapstruct.Mapper;

@Mapper
public interface ProductDynamicValueMapper {
    ProductDynamicValue toEntity(ProductDynamicValueDto dto);
    ProductDynamicValueDto toDto(ProductDynamicValue entity);
}


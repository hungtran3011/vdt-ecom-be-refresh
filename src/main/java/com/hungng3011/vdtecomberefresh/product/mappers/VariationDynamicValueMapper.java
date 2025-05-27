package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import org.mapstruct.Mapper;

@Mapper
public interface VariationDynamicValueMapper {
    VariationDynamicValue toEntity(VariationDynamicValueDto dto);
    VariationDynamicValueDto toDto(VariationDynamicValue entity);
}


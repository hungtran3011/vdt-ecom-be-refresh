package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.product.entities.CategoryDynamicField;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryDynamicFieldMapper {
    CategoryDynamicField toEntity(CategoryDynamicFieldDto dto);
    CategoryDynamicFieldDto toDto(CategoryDynamicField entity);
}


package com.hungng3011.vdtecomberefresh.category.mappers;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryDynamicFieldMapper {
    CategoryDynamicField toEntity(CategoryDynamicFieldDto dto);
    CategoryDynamicFieldDto toDto(CategoryDynamicField entity);
}


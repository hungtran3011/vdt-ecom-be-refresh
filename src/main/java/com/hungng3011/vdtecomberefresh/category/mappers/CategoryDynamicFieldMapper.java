package com.hungng3011.vdtecomberefresh.category.mappers;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryDynamicFieldMapper {
    CategoryDynamicField toEntity(CategoryDynamicFieldDto dto);
    CategoryDynamicFieldDto toDto(CategoryDynamicField entity);
}


package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.CategoryDto;
import com.hungng3011.vdtecomberefresh.product.entities.Category;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryMapper {
    public Category toEntity(CategoryDto categoryDto);
    public CategoryDto toDto(Category category);
}

package com.hungng3011.vdtecomberefresh.category.mappers;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDto;
import com.hungng3011.vdtecomberefresh.category.entities.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {CategoryDynamicFieldMapper.class})
public interface CategoryMapper {
    public Category toEntity(CategoryDto categoryDto);

    @Mapping(target="dynamicFields", source="dynamicFields")
    public CategoryDto toDto(Category category);

    @Named("toDtoWithoutChildren")
    @Mapping(target = "dynamicFields", ignore = true)
    public CategoryDto toDtoWithoutChildren(Category category);

}

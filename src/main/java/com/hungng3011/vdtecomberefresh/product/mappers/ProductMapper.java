package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.category.mappers.CategoryMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper {
    Product toEntity(ProductDto dto);
    @Mapping(target = "category", qualifiedByName = "toDtoWithoutChildren")
    ProductDto toDto(Product entity);
}


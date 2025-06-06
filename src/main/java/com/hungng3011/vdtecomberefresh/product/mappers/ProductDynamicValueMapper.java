package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.ProductDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.entities.ProductDynamicValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductDynamicValueMapper {
    @Mapping(target = "product.id", source = "productId")
    ProductDynamicValue toEntity(ProductDynamicValueDto dto);

    @Mapping(target = "productId", source = "product.id")
    ProductDynamicValueDto toDto(ProductDynamicValue entity);
}


package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VariationDynamicValueMapper {
    @Mapping(target = "variation.id", source = "variationId")
    VariationDynamicValue toEntity(VariationDynamicValueDto dto);

    @Mapping(target = "variationId", source = "variation.id")
    VariationDynamicValueDto toDto(VariationDynamicValue entity);
}


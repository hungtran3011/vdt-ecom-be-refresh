package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VariationMapper {
    @Mapping(source = "productId", target = "product.id")
    Variation toEntity(VariationDto dto);

    @Mapping(source = "product.id", target = "productId")
    VariationDto toDto(Variation entity);
}


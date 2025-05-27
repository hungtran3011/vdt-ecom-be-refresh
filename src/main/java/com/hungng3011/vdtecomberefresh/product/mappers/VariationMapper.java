package com.hungng3011.vdtecomberefresh.product.mappers;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import org.mapstruct.Mapper;

@Mapper
public interface VariationMapper {
    Variation toEntity(VariationDto dto);
    VariationDto toDto(Variation entity);
}


package com.hungng3011.vdtecomberefresh.stock.mappers;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockMapper {
    StockDto toDto(Stock stock);
    
    @Mapping(target = "expectedRestockDate", ignore = true)
    @Mapping(target = "maxPreOrderQuantity", ignore = true)
    @Mapping(target = "preOrderCount", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "history", ignore = true)
    Stock toEntity(StockDto stockDto);
}

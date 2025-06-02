package com.hungng3011.vdtecomberefresh.stock.mappers;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMapper {
    StockDto toDto(Stock stock);
    Stock toEntity(StockDto stockDto);
}

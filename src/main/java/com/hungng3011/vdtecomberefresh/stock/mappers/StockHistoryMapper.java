package com.hungng3011.vdtecomberefresh.stock.mappers;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockHistoryMapper {
    StockHistoryDto toDto(StockHistory stockHistory);
    StockHistory toEntity(StockHistoryDto stockHistoryDto);
}
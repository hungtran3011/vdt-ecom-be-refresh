package com.hungng3011.vdtecomberefresh.stock.mappers;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockHistoryMapper {
    StockHistoryDto toDto(StockHistory stockHistory);
    StockHistory toEntity(StockHistoryDto stockHistoryDto);
}
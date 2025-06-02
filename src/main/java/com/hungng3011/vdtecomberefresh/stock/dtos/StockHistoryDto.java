package com.hungng3011.vdtecomberefresh.stock.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockHistoryDto {
    private Long id;
    private Long stockId;
    private String stockSku;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private StockActionState action;
    private String reference;
    private LocalDateTime timestamp;
    private String updatedBy;
}
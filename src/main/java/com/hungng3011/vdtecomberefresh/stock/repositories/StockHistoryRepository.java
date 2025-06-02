package com.hungng3011.vdtecomberefresh.stock.repositories;

import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByStockIdOrderByTimestampDesc(Long stockId);
}

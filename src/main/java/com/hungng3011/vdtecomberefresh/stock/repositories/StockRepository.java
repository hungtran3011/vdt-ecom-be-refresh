package com.hungng3011.vdtecomberefresh.stock.repositories;

import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByProductId(Long id);
}

package com.hungng3011.vdtecomberefresh.stock;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("v1/stock")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<StockDto> getAll() {
        return stockService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDto> getStockById(@PathVariable Long id) {
        StockDto stock = stockService.getById(id);
        return stock != null ? ResponseEntity.ok(stock) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<StockDto> createStock(@RequestBody StockDto stockDto) {
        return ResponseEntity.ok(stockService.create(stockDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockDto> updateStock(@PathVariable Long id, @RequestBody StockDto stockDto) {
        StockDto updated = stockService.update(id, stockDto);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/restock")
    public ResponseEntity<StockDto> restockItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(stockService.performStockAction(id, quantity, StockActionState.RESTOCK, reference));
    }
    
    @PostMapping("/{id}/sale")
    public ResponseEntity<StockDto> sellItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(stockService.performStockAction(id, quantity, StockActionState.SALE, reference));
    }
    
    @PostMapping("/{id}/return")
    public ResponseEntity<StockDto> returnItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(stockService.performStockAction(id, quantity, StockActionState.RETURN, reference));
    }
    
    @PostMapping("/{id}/adjust")
    public ResponseEntity<StockDto> adjustInventory(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(stockService.performStockAction(id, quantity, StockActionState.ADJUSTMENT, reference));
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StockHistoryDto>> getStockHistory(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getStockHistory(id));
    }
}

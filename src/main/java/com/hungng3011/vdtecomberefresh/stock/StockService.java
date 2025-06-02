package com.hungng3011.vdtecomberefresh.stock;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockHistoryMapper;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockMapper;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockHistoryRepository;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final StockMapper stockMapper;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockHistoryMapper stockHistoryMapper;
    private final Logger logger = LoggerFactory.getLogger(StockService.class);

    public List<StockDto> getAll() {
        return stockRepository.findAll()
                .stream()
                .map(stockMapper::toDto)
                .collect(Collectors.toList());
    }

    public StockDto getById(Long id) {
        return stockRepository.findById(id)
                .map(stockMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public StockDto create(StockDto stockDto) {
        logger.info("Creating stock: {}", stockDto);
        Stock stock = stockMapper.toEntity(stockDto);
        Stock savedStock = stockRepository.save(stock);

        // Create initial history entry
        createHistoryEntry(savedStock, 0, savedStock.getQuantity(),
                StockActionState.RESTOCK, "Initial stock creation");

        return stockMapper.toDto(savedStock);
    }

    @Transactional
    public StockDto update(Long id, StockDto stockDto) {
        logger.info("Updating stock with id {}: {}", id, stockDto);
        if (!stockRepository.existsById(id)) {
            logger.warn("Stock with id {} not found", id);
            return null;
        }

        Stock existingStock = stockRepository.findById(id).orElse(null);
        if (existingStock == null) {
            return null;
        }

        Integer oldQuantity = existingStock.getQuantity();
        stockDto.setId(id);
        Stock stock = stockMapper.toEntity(stockDto);
        Stock updatedStock = stockRepository.save(stock);

        // Create history entry if quantity changed
        if (!oldQuantity.equals(updatedStock.getQuantity())) {
            createHistoryEntry(updatedStock, oldQuantity, updatedStock.getQuantity(),
                    StockActionState.ADJUSTMENT, "Manual update");
        }

        return stockMapper.toDto(updatedStock);
    }

    @Transactional
    public void delete(Long id) {
        logger.info("Deleting stock with id {}", id);
        stockRepository.deleteById(id);
    }

    @Transactional
    public StockDto performStockAction(Long stockId, Integer quantity, StockActionState action, String reference) {
        logger.info("Performing {} action on stock id {} with quantity {}", action, stockId, quantity);
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));

        Integer oldQuantity = stock.getQuantity();
        Integer newQuantity;

        switch (action) {
            case RESTOCK:
                newQuantity = oldQuantity + quantity;
                break;
            case SALE:
                if (oldQuantity < quantity) {
                    throw new IllegalStateException("Insufficient stock. Available: " + oldQuantity + ", Requested: " + quantity);
                }
                newQuantity = oldQuantity - quantity;
                break;
            case RETURN:
                newQuantity = oldQuantity + quantity;
                break;
            case ADJUSTMENT:
                newQuantity = quantity; // Direct set to new value
                break;
            default:
                throw new IllegalArgumentException("Unsupported stock action: " + action);
        }

        stock.setQuantity(newQuantity);
        stock.setUpdatedAt(LocalDateTime.now());

        Stock updatedStock = stockRepository.save(stock);
        createHistoryEntry(updatedStock, oldQuantity, newQuantity, action, reference);

        return stockMapper.toDto(updatedStock);
    }

    @Transactional
    public StockDto setupPreOrder(Long stockId, LocalDate expectedRestockDate, Integer maxPreOrderQuantity) {
        logger.info("Setting up pre-order for stock id {} with expected restock date {} and max quantity {}",
                stockId, expectedRestockDate, maxPreOrderQuantity);

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));
//
//        stock.setExpectedRestockDate(expectedRestockDate);
//        stock.setMaxPreOrderQuantity(maxPreOrderQuantity);
//        stock.setPreOrderCount(0);

        // Force update of status
        stock.updateStatus();

        Stock updatedStock = stockRepository.save(stock);
        createHistoryEntry(updatedStock, updatedStock.getQuantity(), updatedStock.getQuantity(),
                StockActionState.ADJUSTMENT, "Pre-order setup");

        return stockMapper.toDto(updatedStock);
    }

//    @Transactional
//    public StockDto placePreOrder(Long stockId, Integer quantity, String reference) {
//        logger.info("Placing pre-order for stock id {} with quantity {}", stockId, quantity);
//
//        Stock stock = stockRepository.findById(stockId)
//                .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));
//
//        if (stock.getStatus() != StockStatus.PRE_ORDER) {
//            throw new IllegalStateException("Stock is not available for pre-order");
//        }
//
//        Integer remainingPreOrders = stock.getMaxPreOrderQuantity() - stock.getPreOrderCount();
//        if (quantity > remainingPreOrders) {
//            throw new IllegalStateException("Requested pre-order quantity exceeds available slots. Available: "
//                    + remainingPreOrders + ", Requested: " + quantity);
//        }
//
//        Integer oldPreOrderCount = stock.getPreOrderCount();
//        stock.setPreOrderCount(oldPreOrderCount + quantity);
//
//        // Update status if we've reached max pre-orders
//        if (stock.getPreOrderCount() >= stock.getMaxPreOrderQuantity()) {
//            stock.setStatus(StockStatus.OUT_OF_STOCK);
//        }
//
//        Stock updatedStock = stockRepository.save(stock);
//        createHistoryEntry(updatedStock, updatedStock.getQuantity(), updatedStock.getQuantity(),
//                StockActionState.SALE, "Pre-order: " + reference);
//
//        return stockMapper.toDto(updatedStock);
//    }
//
//    @Transactional
//    public StockDto fulfillPreOrders(Long stockId, Integer fulfillQuantity, String reference) {
//        logger.info("Fulfilling pre-orders for stock id {} with quantity {}", stockId, fulfillQuantity);
//
//        Stock stock = stockRepository.findById(stockId)
//                .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));
//
//        if (stock.getPreOrderCount() == null || stock.getPreOrderCount() == 0) {
//            throw new IllegalStateException("No pre-orders to fulfill");
//        }
//
//        // Reset pre-order counter and restock inventory
//        Integer oldQuantity = stock.getQuantity();
//        Integer oldPreOrderCount = stock.getPreOrderCount();
//
//        // Fulfill only up to the actual pre-order count
//        int actualFulfill = Math.min(fulfillQuantity, oldPreOrderCount);
//
//        // If not fulfilling all, reduce pre-order count by fulfilled amount
//        if (actualFulfill < oldPreOrderCount) {
//            stock.setPreOrderCount(oldPreOrderCount - actualFulfill);
//        } else {
//            // If fulfilling all, reset pre-order settings
//            stock.setPreOrderCount(0);
//            stock.setMaxPreOrderQuantity(null);
//            stock.setExpectedRestockDate(null);
//        }
//
//        // Add inventory
//        stock.setQuantity(oldQuantity + actualFulfill);
//
//        Stock updatedStock = stockRepository.save(stock);
//        createHistoryEntry(updatedStock, oldQuantity, updatedStock.getQuantity(),
//                StockActionState.RESTOCK, "Pre-order fulfillment: " + reference);
//
//        return stockMapper.toDto(updatedStock);
//    }

    private void createHistoryEntry(Stock stock, Integer quantityBefore, Integer quantityAfter,
                                    StockActionState action, String reference) {
        StockHistory history = new StockHistory();
        history.setStock(stock);
        history.setQuantityBefore(quantityBefore);
        history.setQuantityAfter(quantityAfter);
        history.setAction(action);
        history.setReference(reference);
        history.setTimestamp(LocalDateTime.now());

        // Get current username if available
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.isAuthenticated()) {
        //     history.setUpdatedBy(authentication.getName());
        // } else {
        //     history.setUpdatedBy("system");
        // }

        stockHistoryRepository.save(history);
        logger.info("Created stock history entry: {}", history);
    }

    public List<StockHistoryDto> getStockHistory(Long stockId) {
        logger.info("Getting history for stock id {}", stockId);
        return stockHistoryRepository.findByStockIdOrderByTimestampDesc(stockId)
                .stream()
                .map(stockHistoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public void removeStockByProductId(Long id) {
        logger.info("Removing stock for product id {}", id);
        List<Stock> stocks = stockRepository.findByProductId(id);
        if (stocks.isEmpty()) {
            logger.warn("No stock found for product id {}", id);
            return;
        }

        for (Stock stock : stocks) {
            stockRepository.delete(stock);
            logger.info("Deleted stock with id {}", stock.getId());
        }
    }
//
//    public boolean isPreOrderAvailable(Long stockId) {
//        Stock stock = stockRepository.findById(stockId).orElse(null);
//        if (stock == null) {
//            return false;
//        }
//        return stock.getStatus() == StockStatus.PRE_ORDER &&
//                stock.getPreOrderCount() < stock.getMaxPreOrderQuantity();
//    }
//
//    public Integer getRemainingPreOrders(Long stockId) {
//        Stock stock = stockRepository.findById(stockId).orElse(null);
//        if (stock == null || stock.getStatus() != StockStatus.PRE_ORDER) {
//            return 0;
//        }
//        return stock.getMaxPreOrderQuantity() - stock.getPreOrderCount();
//    }
}
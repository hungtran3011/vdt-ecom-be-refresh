package com.hungng3011.vdtecomberefresh.stock;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.AvailableVariationDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.VariationCombinationDto;
import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockHistoryMapper;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockMapper;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockHistoryRepository;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockRepository;
import com.hungng3011.vdtecomberefresh.product.mappers.VariationMapper;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final StockMapper stockMapper;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockHistoryMapper stockHistoryMapper;
    private final VariationMapper variationMapper;
    private final ProductRepository productRepository;

    public List<StockDto> getAll() {
        try {
            log.info("Retrieving all stock items");
            List<StockDto> stocks = stockRepository.findAll()
                    .stream()
                    .map(stockMapper::toDto)
                    .collect(Collectors.toList());
            log.info("Retrieved {} stock items", stocks.size());
            return stocks;
        } catch (Exception e) {
            log.error("Error retrieving all stock items", e);
            throw e;
        }
    }

    public StockDto getById(Long id) {
        try {
            log.info("Retrieving stock item with id: {}", id);
            StockDto stock = stockRepository.findById(id)
                    .map(stockMapper::toDto)
                    .orElse(null);
            if (stock != null) {
                log.info("Found stock item with id: {}", id);
            } else {
                log.warn("Stock item not found with id: {}", id);
            }
            return stock;
        } catch (Exception e) {
            log.error("Error retrieving stock item with id: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public StockDto create(StockDto stockDto) {
        try {
            log.info("Creating stock for product id: {} with quantity: {}", 
                    stockDto.getProductId(), stockDto.getQuantity());
            
            // Fetch the product entity
            Product product = productRepository.findById(stockDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + stockDto.getProductId()));
            
            Stock stock = stockMapper.toEntity(stockDto);
            stock.setProduct(product); // Manually set the product entity
            
            Stock savedStock = stockRepository.save(stock);
            
            // Create initial history entry
            createHistoryEntry(savedStock, 0, savedStock.getQuantity(),
                    StockActionState.RESTOCK, "Initial stock creation");
            
            log.info("Successfully created stock with id: {} for product: {}", 
                    savedStock.getId(), savedStock.getProduct().getId());
            return stockMapper.toDto(savedStock);
        } catch (Exception e) {
            log.error("Error creating stock for product id: {}", stockDto.getProductId(), e);
            throw e;
        }
    }

    @Transactional
    public StockDto update(Long id, StockDto stockDto) {
        try {
            log.info("Updating stock with id: {} for product: {}", id, stockDto.getProductId());
            if (!stockRepository.existsById(id)) {
                log.warn("Stock with id {} not found for update", id);
                return null;
            }

            Stock existingStock = stockRepository.findById(id).orElse(null);
            if (existingStock == null) {
                log.warn("Stock with id {} not found in database", id);
                return null;
            }

            Integer oldQuantity = existingStock.getQuantity();
            stockDto.setId(id);
            
            // Fetch the product entity if productId is provided
            Stock stock = stockMapper.toEntity(stockDto);
            if (stockDto.getProductId() != null) {
                Product product = productRepository.findById(stockDto.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + stockDto.getProductId()));
                stock.setProduct(product);
            } else {
                // Keep existing product if no productId provided
                stock.setProduct(existingStock.getProduct());
            }
            
            Stock updatedStock = stockRepository.save(stock);

            // Create history entry if quantity changed
            if (!oldQuantity.equals(updatedStock.getQuantity())) {
                log.info("Stock quantity changed from {} to {} for stock id: {}", 
                        oldQuantity, updatedStock.getQuantity(), id);
                createHistoryEntry(updatedStock, oldQuantity, updatedStock.getQuantity(),
                        StockActionState.ADJUSTMENT, "Manual update");
            }

            log.info("Successfully updated stock with id: {}", id);
            return stockMapper.toDto(updatedStock);
        } catch (Exception e) {
            log.error("Error updating stock with id: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public void delete(Long id) {
        try {
            log.info("Deleting stock with id: {}", id);
            if (!stockRepository.existsById(id)) {
                log.warn("Stock with id {} not found for deletion", id);
                return;
            }
            stockRepository.deleteById(id);
            log.info("Successfully deleted stock with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting stock with id: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public StockDto performStockAction(Long stockId, Integer quantity, StockActionState action, String reference) {
        try {
            log.info("Performing {} action on stock id: {} with quantity: {}, reference: {}", 
                    action, stockId, quantity, reference);
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));

            Integer oldQuantity = stock.getQuantity();
            Integer newQuantity;

            switch (action) {
                case RESTOCK:
                    newQuantity = oldQuantity + quantity;
                    log.info("Restocking stock id: {} from {} to {}", stockId, oldQuantity, newQuantity);
                    break;
                case SALE:
                    if (oldQuantity < quantity) {
                        log.warn("Insufficient stock for sale. Stock id: {}, Available: {}, Requested: {}", 
                                stockId, oldQuantity, quantity);
                        throw new IllegalStateException("Insufficient stock. Available: " + oldQuantity + ", Requested: " + quantity);
                    }
                    newQuantity = oldQuantity - quantity;
                    log.info("Sale from stock id: {} from {} to {}", stockId, oldQuantity, newQuantity);
                    break;
                case RETURN:
                    newQuantity = oldQuantity + quantity;
                    log.info("Return to stock id: {} from {} to {}", stockId, oldQuantity, newQuantity);
                    break;
                case ADJUSTMENT:
                    newQuantity = quantity; // Direct set to new value
                    log.info("Adjusting stock id: {} from {} to {}", stockId, oldQuantity, newQuantity);
                    break;
                default:
                    log.error("Unsupported stock action: {} for stock id: {}", action, stockId);
                    throw new IllegalArgumentException("Unsupported stock action: " + action);
            }

            stock.setQuantity(newQuantity);
            stock.setUpdatedAt(LocalDateTime.now());

            Stock updatedStock = stockRepository.save(stock);
            createHistoryEntry(updatedStock, oldQuantity, newQuantity, action, reference);

            log.info("Successfully performed {} action on stock id: {}", action, stockId);
            return stockMapper.toDto(updatedStock);
        } catch (Exception e) {
            log.error("Error performing {} action on stock id: {}", action, stockId, e);
            throw e;
        }
    }

    @Transactional
    public StockDto setupPreOrder(Long stockId, LocalDate expectedRestockDate, Integer maxPreOrderQuantity) {
        try {
            log.info("Setting up pre-order for stock id: {} with expected restock date: {} and max quantity: {}",
                    stockId, expectedRestockDate, maxPreOrderQuantity);

            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));

            // Force update of status
            stock.updateStatus();

            Stock updatedStock = stockRepository.save(stock);
            createHistoryEntry(updatedStock, updatedStock.getQuantity(), updatedStock.getQuantity(),
                    StockActionState.ADJUSTMENT, "Pre-order setup");

            log.info("Successfully set up pre-order for stock id: {}", stockId);
            return stockMapper.toDto(updatedStock);
        } catch (Exception e) {
            log.error("Error setting up pre-order for stock id: {}", stockId, e);
            throw e;
        }
    }

    private void createHistoryEntry(Stock stock, Integer quantityBefore, Integer quantityAfter,
                                    StockActionState action, String reference) {
        try {
            StockHistory history = new StockHistory();
            history.setStock(stock);
            history.setQuantityBefore(quantityBefore);
            history.setQuantityAfter(quantityAfter);
            history.setAction(action);
            history.setReference(reference);
            history.setTimestamp(LocalDateTime.now());

            stockHistoryRepository.save(history);
            log.info("Created stock history entry for stock id: {} - {} from {} to {}", 
                    stock.getId(), action, quantityBefore, quantityAfter);
        } catch (Exception e) {
            log.error("Error creating stock history entry for stock id: {}", stock.getId(), e);
            throw e;
        }
    }

    public List<StockHistoryDto> getStockHistory(Long stockId) {
        try {
            log.info("Getting history for stock id: {}", stockId);
            List<StockHistoryDto> history = stockHistoryRepository.findByStockIdOrderByTimestampDesc(stockId)
                    .stream()
                    .map(stockHistoryMapper::toDto)
                    .collect(Collectors.toList());
            log.info("Retrieved {} history entries for stock id: {}", history.size(), stockId);
            return history;
        } catch (Exception e) {
            log.error("Error getting history for stock id: {}", stockId, e);
            throw e;
        }
    }

    @Transactional
    public void removeStockByProductId(Long id) {
        try {
            log.info("Removing stock for product id: {}", id);
            List<Stock> stocks = stockRepository.findByProductId(id);
            if (stocks.isEmpty()) {
                log.warn("No stock found for product id: {}", id);
                return;
            }

            for (Stock stock : stocks) {
                stockRepository.delete(stock);
                log.info("Deleted stock with id: {} for product id: {}", stock.getId(), id);
            }
            log.info("Successfully removed all stock for product id: {}", id);
        } catch (Exception e) {
            log.error("Error removing stock for product id: {}", id, e);
            throw e;
        }
    }

    /**
     * Find stock by product and specific variation combination
     * This supports your requirement for variation-specific stock checking
     */
    public List<StockDto> getByProductAndVariations(Long productId, List<Long> variationIds) {
        try {
            log.info("Finding stock for product {} with variations {}", productId, variationIds);
            
            if (variationIds == null || variationIds.isEmpty()) {
                // Return all stock for product if no variations specified
                return getByProductId(productId);
            }
            
            List<Stock> stocks = stockRepository.findByProductIdAndVariationIds(
                productId, variationIds, variationIds.size());
            
            List<StockDto> result = stocks.stream()
                    .map(stockMapper::toDto)
                    .collect(Collectors.toList());
            
            log.info("Found {} stock items for product {} with specified variations", result.size(), productId);
            return result;
        } catch (Exception e) {
            log.error("Error finding stock for product {} with variations {}", productId, variationIds, e);
            throw e;
        }
    }
    
    /**
     * Check if a specific variation combination is available for a product
     */
    public boolean isVariationCombinationAvailable(Long productId, List<Long> variationIds) {
        try {
            log.info("Checking availability for product {} with variations {}", productId, variationIds);
            
            if (variationIds == null || variationIds.isEmpty()) {
                return !getByProductId(productId).isEmpty();
            }
            
            List<Stock> availableStocks = stockRepository.findAvailableByProductIdAndVariationIds(
                productId, variationIds, variationIds.size());
            
            boolean available = !availableStocks.isEmpty();
            log.info("Variation combination available: {} for product {} with variations {}", 
                    available, productId, variationIds);
            return available;
        } catch (Exception e) {
            log.error("Error checking availability for product {} with variations {}", productId, variationIds, e);
            return false;
        }
    }
    
    /**
     * Get all available variation combinations for a product
     * Returns stock items that have quantity > 0
     */
    public List<StockDto> getAvailableVariationsForProduct(Long productId) {
        try {
            log.info("Getting available variations for product {}", productId);
            
            List<Stock> availableStocks = stockRepository.findAvailableVariationsByProductId(productId);
            
            List<StockDto> result = availableStocks.stream()
                    .map(stockMapper::toDto)
                    .collect(Collectors.toList());
            
            log.info("Found {} available variation combinations for product {}", result.size(), productId);
            return result;
        } catch (Exception e) {
            log.error("Error getting available variations for product {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Find stock by SKU - useful for cart and order operations
     */
    public StockDto getBySku(String sku) {
        try {
            log.info("Finding stock by SKU: {}", sku);
            
            return stockRepository.findBySku(sku)
                    .map(stockMapper::toDto)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error finding stock by SKU: {}", sku, e);
            throw e;
        }
    }
    
    /**
     * Validate stock availability for order/cart operations
     */
    public boolean validateStockForVariationCombination(Long productId, List<Long> variationIds, Integer requiredQuantity) {
        try {
            log.info("Validating stock for product {} with variations {} and quantity {}", 
                    productId, variationIds, requiredQuantity);
            
            List<Stock> stocks = stockRepository.findAvailableByProductIdAndVariationIds(
                productId, variationIds, variationIds.size());
            
            if (stocks.isEmpty()) {
                log.warn("No stock found for product {} with variations {}", productId, variationIds);
                return false;
            }
            
            // Check if any stock has sufficient quantity
            boolean sufficient = stocks.stream()
                    .anyMatch(stock -> stock.getQuantity() >= requiredQuantity);
            
            log.info("Stock validation result: {} for product {} with variations {} and quantity {}", 
                    sufficient, productId, variationIds, requiredQuantity);
            return sufficient;
        } catch (Exception e) {
            log.error("Error validating stock for product {} with variations {} and quantity {}", 
                    productId, variationIds, requiredQuantity, e);
            return false;
        }
    }
    
    /**
     * Get stock by product ID (existing method - keeping for compatibility)
     */
    public List<StockDto> getByProductId(Long productId) {
        try {
            log.info("Retrieving stock for product id: {}", productId);
            List<Stock> stocks = stockRepository.findByProductId(productId);
            List<StockDto> result = stocks.stream()
                    .map(stockMapper::toDto)
                    .collect(Collectors.toList());
            log.info("Found {} stock items for product id: {}", result.size(), productId);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving stock for product id: {}", productId, e);
            throw e;
        }
    }

    /**
     * Get available variation combinations with detailed information for frontend
     * This provides all the data frontend needs to display available options
     */
    public List<AvailableVariationDto> getAvailableVariationDetails(Long productId) {
        try {
            log.info("Getting detailed available variations for product {}", productId);
            
            List<Stock> availableStocks = stockRepository.findAvailableVariationsByProductId(productId);
            
            List<AvailableVariationDto> result = availableStocks.stream()
                    .map(stock -> {
                        AvailableVariationDto dto = new AvailableVariationDto();
                        dto.setStockId(stock.getId());
                        dto.setSku(stock.getSku());
                        dto.setProductId(stock.getProduct().getId());
                        dto.setProductName(stock.getProduct().getName());
                        dto.setVariations(stock.getVariations().stream()
                                .map(variationMapper::toDto)
                                .collect(Collectors.toList()));
                        dto.setAvailableQuantity(stock.getQuantity());
                        dto.setStatus(stock.getStatus());
                        dto.setAvailable(stock.getQuantity() > 0 && 
                                (stock.getStatus() == StockStatus.IN_STOCK || stock.getStatus() == StockStatus.LOW_STOCK));
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            log.info("Found {} detailed available variations for product {}", result.size(), productId);
            return result;
        } catch (Exception e) {
            log.error("Error getting detailed available variations for product {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Batch validate multiple variation combinations
     * Useful for cart validation or bulk operations
     */
    public Map<VariationCombinationDto, Boolean> validateMultipleVariationCombinations(
            List<VariationCombinationDto> combinations) {
        try {
            log.info("Validating {} variation combinations", combinations.size());
            
            Map<VariationCombinationDto, Boolean> results = new HashMap<>();
            
            for (VariationCombinationDto combination : combinations) {
                boolean valid = validateStockForVariationCombination(
                        combination.getProductId(), 
                        combination.getVariationIds(), 
                        combination.getQuantity() != null ? combination.getQuantity() : 1);
                results.put(combination, valid);
            }
            
            log.info("Completed validation of {} variation combinations", combinations.size());
            return results;
        } catch (Exception e) {
            log.error("Error validating multiple variation combinations", e);
            throw e;
        }
    }
}
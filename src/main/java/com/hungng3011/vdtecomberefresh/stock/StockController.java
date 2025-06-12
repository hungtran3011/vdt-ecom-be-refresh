package com.hungng3011.vdtecomberefresh.stock;

import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.AvailableVariationDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.VariationCombinationDto;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/stock")
@RequiredArgsConstructor
@Slf4j
public class StockController {
    private final StockService stockService;

    @GetMapping
    public List<StockDto> getAll() {
        log.info("Fetching all stock items");
        try {
            List<StockDto> stocks = stockService.getAll();
            log.info("Successfully retrieved {} stock items", stocks.size());
            return stocks;
        } catch (Exception e) {
            log.error("Error fetching all stock items", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDto> getStockById(@PathVariable Long id) {
        log.info("Fetching stock item with ID: {}", id);
        try {
            StockDto stock = stockService.getById(id);
            if (stock != null) {
                log.info("Successfully retrieved stock item with ID: {}", id);
                return ResponseEntity.ok(stock);
            } else {
                log.warn("Stock item not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching stock item with ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<StockDto> createStock(@RequestBody StockDto stockDto) {
        log.info("Creating new stock item for product ID: {}", stockDto.getProductId());
        try {
            StockDto result = stockService.create(stockDto);
            log.info("Successfully created stock item with ID: {}", result.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating stock item for product ID: {}", stockDto.getProductId(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockDto> updateStock(@PathVariable Long id, @RequestBody StockDto stockDto) {
        log.info("Updating stock item with ID: {}", id);
        try {
            StockDto updated = stockService.update(id, stockDto);
            if (updated != null) {
                log.info("Successfully updated stock item with ID: {}", id);
                return ResponseEntity.ok(updated);
            } else {
                log.warn("Stock item not found for update with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error updating stock item with ID: {}", id, e);
            throw e;
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        log.info("Deleting stock item with ID: {}", id);
        try {
            stockService.delete(id);
            log.info("Successfully deleted stock item with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting stock item with ID: {}", id, e);
            throw e;
        }
    }
    
    @PostMapping("/{id}/restock")
    public ResponseEntity<StockDto> restockItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        log.info("Restocking item with ID: {}, quantity: {}, reference: {}", id, quantity, reference);
        try {
            StockDto result = stockService.performStockAction(id, quantity, StockActionState.RESTOCK, reference);
            log.info("Successfully restocked item with ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error restocking item with ID: {}", id, e);
            throw e;
        }
    }
    
    @PostMapping("/{id}/sale")
    public ResponseEntity<StockDto> sellItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        log.info("Processing sale for item with ID: {}, quantity: {}, reference: {}", id, quantity, reference);
        try {
            StockDto result = stockService.performStockAction(id, quantity, StockActionState.SALE, reference);
            log.info("Successfully processed sale for item with ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing sale for item with ID: {}", id, e);
            throw e;
        }
    }
    
    @PostMapping("/{id}/return")
    public ResponseEntity<StockDto> returnItem(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        log.info("Processing return for item with ID: {}, quantity: {}, reference: {}", id, quantity, reference);
        try {
            StockDto result = stockService.performStockAction(id, quantity, StockActionState.RETURN, reference);
            log.info("Successfully processed return for item with ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing return for item with ID: {}", id, e);
            throw e;
        }
    }
    
    @PostMapping("/{id}/adjust")
    public ResponseEntity<StockDto> adjustInventory(
            @PathVariable Long id, 
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference) {
        log.info("Adjusting inventory for item with ID: {}, quantity: {}, reference: {}", id, quantity, reference);
        try {
            StockDto result = stockService.performStockAction(id, quantity, StockActionState.ADJUSTMENT, reference);
            log.info("Successfully adjusted inventory for item with ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error adjusting inventory for item with ID: {}", id, e);
            throw e;
        }
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StockHistoryDto>> getStockHistory(@PathVariable Long id) {
        log.info("Fetching stock history for item with ID: {}", id);
        try {
            List<StockHistoryDto> history = stockService.getStockHistory(id);
            log.info("Successfully retrieved {} stock history entries for item with ID: {}", history.size(), id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching stock history for item with ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Get stock by product and specific variation combination
     * Endpoint: GET /v1/stock/product/{productId}/variations?variationIds=1,2,3
     */
    @GetMapping("/product/{productId}/variations")
    public ResponseEntity<List<StockDto>> getStockByProductAndVariations(
            @PathVariable Long productId,
            @RequestParam(required = false) List<Long> variationIds) {
        log.info("Fetching stock for product {} with variations {}", productId, variationIds);
        try {
            List<StockDto> stocks = stockService.getByProductAndVariations(productId, variationIds);
            log.info("Successfully retrieved {} stock items for product {} with variations", stocks.size(), productId);
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            log.error("Error fetching stock for product {} with variations {}", productId, variationIds, e);
            throw e;
        }
    }
    
    /**
     * Check if variation combination is available for a product
     * Endpoint: GET /v1/stock/product/{productId}/variations/available?variationIds=1,2,3
     */
    @GetMapping("/product/{productId}/variations/available")
    public ResponseEntity<Boolean> checkVariationAvailability(
            @PathVariable Long productId,
            @RequestParam(required = false) List<Long> variationIds) {
        log.info("Checking availability for product {} with variations {}", productId, variationIds);
        try {
            boolean available = stockService.isVariationCombinationAvailable(productId, variationIds);
            log.info("Availability check result: {} for product {} with variations {}", available, productId, variationIds);
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            log.error("Error checking availability for product {} with variations {}", productId, variationIds, e);
            throw e;
        }
    }
    
    /**
     * Get all available variation combinations for a product
     * Endpoint: GET /v1/stock/product/{productId}/available-variations
     */
    @GetMapping("/product/{productId}/available-variations")
    public ResponseEntity<List<StockDto>> getAvailableVariationsForProduct(@PathVariable Long productId) {
        log.info("Fetching available variations for product {}", productId);
        try {
            List<StockDto> availableVariations = stockService.getAvailableVariationsForProduct(productId);
            log.info("Successfully retrieved {} available variations for product {}", availableVariations.size(), productId);
            return ResponseEntity.ok(availableVariations);
        } catch (Exception e) {
            log.error("Error fetching available variations for product {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Find stock by SKU
     * Endpoint: GET /v1/stock/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<StockDto> getStockBySku(@PathVariable String sku) {
        log.info("Fetching stock by SKU: {}", sku);
        try {
            StockDto stock = stockService.getBySku(sku);
            if (stock != null) {
                log.info("Successfully retrieved stock for SKU: {}", sku);
                return ResponseEntity.ok(stock);
            } else {
                log.warn("Stock not found for SKU: {}", sku);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching stock by SKU: {}", sku, e);
            throw e;
        }
    }
    
    /**
     * Simple stock validation for product without variations - matches frontend expectation
     * Endpoint: POST /v1/stock/product/{productId}/validate
     */
    @PostMapping("/product/{productId}/validate")
    public ResponseEntity<Map<String, Object>> validateProductStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        log.info("Validating stock for product {} with quantity {}", productId, quantity);
        try {
            // Get stock for product
            List<StockDto> stocks = stockService.getByProductId(productId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (stocks.isEmpty()) {
                response.put("available", false);
                response.put("availableQuantity", 0);
                response.put("message", "No stock found for this product");
            } else {
                StockDto stock = stocks.get(0); // Get first stock item
                boolean available = stock.getQuantity() >= quantity;
                response.put("available", available);
                response.put("availableQuantity", stock.getQuantity());
                if (!available) {
                    response.put("message", "Insufficient stock. Available: " + stock.getQuantity() + ", Requested: " + quantity);
                }
            }
            
            log.info("Stock validation result: {} for product {} with quantity {}", 
                    response.get("available"), productId, quantity);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating stock for product {} with quantity {}", productId, quantity, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("available", false);
            errorResponse.put("availableQuantity", 0);
            errorResponse.put("message", "Error validating stock: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get detailed available variations for a product (enhanced for frontend)
     * Endpoint: GET /v1/stock/product/{productId}/available-variations/detailed
     */
    @GetMapping("/product/{productId}/available-variations/detailed")
    public ResponseEntity<List<AvailableVariationDto>> getDetailedAvailableVariations(@PathVariable Long productId) {
        log.info("Fetching detailed available variations for product {}", productId);
        try {
            List<AvailableVariationDto> detailedVariations = stockService.getAvailableVariationDetails(productId);
            log.info("Successfully retrieved {} detailed available variations for product {}", 
                    detailedVariations.size(), productId);
            return ResponseEntity.ok(detailedVariations);
        } catch (Exception e) {
            log.error("Error fetching detailed available variations for product {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Batch validate multiple variation combinations
     * Endpoint: POST /v1/stock/validate-batch
     */
    @PostMapping("/validate-batch")
    public ResponseEntity<Map<VariationCombinationDto, Boolean>> batchValidateVariationCombinations(
            @RequestBody List<VariationCombinationDto> combinations) {
        log.info("Batch validating {} variation combinations", combinations.size());
        try {
            Map<VariationCombinationDto, Boolean> results = stockService.validateMultipleVariationCombinations(combinations);
            log.info("Successfully validated {} variation combinations", combinations.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error batch validating variation combinations", e);
            throw e;
        }
    }
}

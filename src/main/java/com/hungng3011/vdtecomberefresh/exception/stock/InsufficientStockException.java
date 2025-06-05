package com.hungng3011.vdtecomberefresh.exception.stock;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when stock operations fail
 */
public class InsufficientStockException extends BaseBusinessException {
    
    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", message, HttpStatus.BAD_REQUEST);
    }
    
    public InsufficientStockException(Long stockId, Integer requested, Integer available) {
        super("INSUFFICIENT_STOCK", 
              String.format("Insufficient stock for item %s. Requested: %d, Available: %d", stockId, requested, available), 
              HttpStatus.BAD_REQUEST, stockId, requested, available);
    }
    
    public InsufficientStockException(String productName, Integer requested, Integer available) {
        super("INSUFFICIENT_STOCK", 
              String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d", productName, requested, available), 
              HttpStatus.BAD_REQUEST, productName, requested, available);
    }
}

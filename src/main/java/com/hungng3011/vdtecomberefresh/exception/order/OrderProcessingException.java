package com.hungng3011.vdtecomberefresh.exception.order;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when order operations fail
 */
public class OrderProcessingException extends BaseBusinessException {
    
    public OrderProcessingException(String message) {
        super("ORDER_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public OrderProcessingException(String message, Throwable cause) {
        super("ORDER_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public OrderProcessingException(String message, String orderId) {
        super("ORDER_PROCESSING_ERROR", 
              String.format("Order processing failed for order %s: %s", orderId, message), 
              HttpStatus.BAD_REQUEST, orderId);
    }
}

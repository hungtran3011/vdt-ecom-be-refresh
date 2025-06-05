package com.hungng3011.vdtecomberefresh.exception.payment;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when refund operation fails
 */
public class RefundException extends BaseBusinessException {
    
    public RefundException(String message) {
        super("REFUND_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public RefundException(String message, Throwable cause) {
        super("REFUND_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public RefundException(String message, String orderId) {
        super("REFUND_ERROR", 
              String.format("Refund failed for order %s: %s", orderId, message), 
              HttpStatus.BAD_REQUEST, orderId);
    }
    
    public RefundException(String message, String orderId, Throwable cause) {
        super("REFUND_ERROR", 
              String.format("Refund failed for order %s: %s", orderId, message), 
              HttpStatus.BAD_REQUEST, cause, orderId);
    }
}

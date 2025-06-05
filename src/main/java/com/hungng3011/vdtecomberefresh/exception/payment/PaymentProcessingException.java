package com.hungng3011.vdtecomberefresh.exception.payment;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment operation fails
 */
public class PaymentProcessingException extends BaseBusinessException {
    
    public PaymentProcessingException(String message) {
        super("PAYMENT_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super("PAYMENT_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public PaymentProcessingException(String message, String orderId) {
        super("PAYMENT_PROCESSING_ERROR", 
              String.format("Payment processing failed for order %s: %s", orderId, message), 
              HttpStatus.BAD_REQUEST, orderId);
    }
    
    public PaymentProcessingException(String message, String orderId, Throwable cause) {
        super("PAYMENT_PROCESSING_ERROR", 
              String.format("Payment processing failed for order %s: %s", orderId, message), 
              HttpStatus.BAD_REQUEST, cause, orderId);
    }
}

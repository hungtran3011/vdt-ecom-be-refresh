package com.hungng3011.vdtecomberefresh.exception.cart;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when cart operations fail
 */
public class CartProcessingException extends BaseBusinessException {
    
    public CartProcessingException(String message) {
        super("CART_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public CartProcessingException(String message, Throwable cause) {
        super("CART_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public CartProcessingException(String message, Long cartId) {
        super("CART_PROCESSING_ERROR", 
              String.format("Cart processing failed for cart %s: %s", cartId, message), 
              HttpStatus.BAD_REQUEST, cartId);
    }
    
    public CartProcessingException(String message, HttpStatus status) {
        super("CART_PROCESSING_ERROR", message, status);
    }
}

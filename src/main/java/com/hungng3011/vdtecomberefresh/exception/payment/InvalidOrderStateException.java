package com.hungng3011.vdtecomberefresh.exception.payment;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when order is in invalid state for payment operations
 */
public class InvalidOrderStateException extends BaseBusinessException {
    
    public InvalidOrderStateException(String message) {
        super("INVALID_ORDER_STATE", message, HttpStatus.BAD_REQUEST);
    }
    
    public InvalidOrderStateException(String orderId, String currentState, String requiredState) {
        super("INVALID_ORDER_STATE", 
              String.format("Order %s is in state '%s' but requires state '%s'", orderId, currentState, requiredState), 
              HttpStatus.BAD_REQUEST, orderId, currentState, requiredState);
    }
}

package com.hungng3011.vdtecomberefresh.exception.payment;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment gateway integration fails
 */
public class PaymentGatewayException extends BaseBusinessException {
    
    public PaymentGatewayException(String message) {
        super("PAYMENT_GATEWAY_ERROR", message, HttpStatus.BAD_GATEWAY);
    }
    
    public PaymentGatewayException(String message, Throwable cause) {
        super("PAYMENT_GATEWAY_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }
    
    public PaymentGatewayException(String gatewayName, String message) {
        super("PAYMENT_GATEWAY_ERROR", 
              String.format("%s gateway error: %s", gatewayName, message), 
              HttpStatus.BAD_GATEWAY, gatewayName);
    }
    
    public PaymentGatewayException(String gatewayName, String message, String orderId) {
        super("PAYMENT_GATEWAY_ERROR", 
              String.format("%s gateway error for order %s: %s", gatewayName, orderId, message), 
              HttpStatus.BAD_GATEWAY, gatewayName, orderId);
    }
}

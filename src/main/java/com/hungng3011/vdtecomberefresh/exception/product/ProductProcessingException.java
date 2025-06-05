package com.hungng3011.vdtecomberefresh.exception.product;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product operations fail
 */
public class ProductProcessingException extends BaseBusinessException {
    
    public ProductProcessingException(String message) {
        super("PRODUCT_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public ProductProcessingException(String message, Throwable cause) {
        super("PRODUCT_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public ProductProcessingException(String message, Long productId) {
        super("PRODUCT_PROCESSING_ERROR", 
              String.format("Product processing failed for product %s: %s", productId, message), 
              HttpStatus.BAD_REQUEST, productId);
    }
    
    public ProductProcessingException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
    
    public ProductProcessingException(String errorCode, String message, Object... messageArgs) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, messageArgs);
    }
    
    public ProductProcessingException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
}

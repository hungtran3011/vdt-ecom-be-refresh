package com.hungng3011.vdtecomberefresh.exception.category;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when category operations fail
 */
public class CategoryProcessingException extends BaseBusinessException {
    
    public CategoryProcessingException(String message) {
        super("CATEGORY_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public CategoryProcessingException(String message, Throwable cause) {
        super("CATEGORY_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public CategoryProcessingException(String message, Long categoryId) {
        super("CATEGORY_PROCESSING_ERROR", 
              String.format("Category processing failed for category %s: %s", categoryId, message), 
              HttpStatus.BAD_REQUEST, categoryId);
    }
    
    public CategoryProcessingException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
    
    public CategoryProcessingException(String errorCode, String message, Object... messageArgs) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, messageArgs);
    }
    
    public CategoryProcessingException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
}

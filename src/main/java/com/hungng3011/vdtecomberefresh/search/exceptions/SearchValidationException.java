package com.hungng3011.vdtecomberefresh.search.exceptions;

/**
 * Exception thrown when search request validation fails
 */
public class SearchValidationException extends SearchException {
    
    public SearchValidationException(String message) {
        super(message);
    }
    
    public SearchValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

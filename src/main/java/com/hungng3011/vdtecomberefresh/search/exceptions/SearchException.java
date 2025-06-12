package com.hungng3011.vdtecomberefresh.search.exceptions;

/**
 * Base exception for search-related errors
 */
public class SearchException extends RuntimeException {
    
    public SearchException(String message) {
        super(message);
    }
    
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}

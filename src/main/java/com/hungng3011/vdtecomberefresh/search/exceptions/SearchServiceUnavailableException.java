package com.hungng3011.vdtecomberefresh.search.exceptions;

/**
 * Exception thrown when Elasticsearch is unavailable or fails
 */
public class SearchServiceUnavailableException extends SearchException {
    
    public SearchServiceUnavailableException(String message) {
        super(message);
    }
    
    public SearchServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

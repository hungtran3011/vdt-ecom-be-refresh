package com.hungng3011.vdtecomberefresh.exception.profile;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when profile operations fail
 */
public class ProfileProcessingException extends BaseBusinessException {
    
    public ProfileProcessingException(String message) {
        super("PROFILE_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public ProfileProcessingException(String message, Throwable cause) {
        super("PROFILE_PROCESSING_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
    
    // public ProfileProcessingException(String message, String userId) {
    //     super("PROFILE_PROCESSING_ERROR", 
    //           String.format("Profile processing failed for user %s: %s", userId, message), 
    //           HttpStatus.BAD_REQUEST, userId);
    // }
    
    public ProfileProcessingException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
    
    public ProfileProcessingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, cause);
    }
    
    public ProfileProcessingException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }
    
    public ProfileProcessingException(String errorCode, String message, HttpStatus status, Throwable cause) {
        super(errorCode, message, status, cause);
    }
}

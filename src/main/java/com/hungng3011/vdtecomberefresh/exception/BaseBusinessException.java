package com.hungng3011.vdtecomberefresh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all business exceptions in the application
 */
@Getter
public abstract class BaseBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] messageArgs;
    
    protected BaseBusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = null;
    }
    
    protected BaseBusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = null;
    }
    
    protected BaseBusinessException(String errorCode, String message, HttpStatus httpStatus, Object... messageArgs) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = messageArgs;
    }
    
    protected BaseBusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause, Object... messageArgs) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = messageArgs;
    }
}

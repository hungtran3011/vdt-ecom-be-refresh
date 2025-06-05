package com.hungng3011.vdtecomberefresh.exception.notification;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when notification operations fail
 */
public class NotificationException extends BaseBusinessException {
    
    public NotificationException(String message) {
        super("NOTIFICATION_ERROR", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    public NotificationException(String message, Throwable cause) {
        super("NOTIFICATION_ERROR", message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
    
    public NotificationException(String notificationType, String recipient, String reason) {
        super("NOTIFICATION_ERROR", 
              String.format("Failed to send %s notification to %s: %s", notificationType, recipient, reason), 
              HttpStatus.SERVICE_UNAVAILABLE, notificationType, recipient, reason);
    }
}

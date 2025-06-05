package com.hungng3011.vdtecomberefresh.mail.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDto {
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String template;
    private String htmlContent;
    private String textContent;
    private Map<String, Object> variables;
    private String orderId;
    private String transactionId;
    private String userId;
    private EmailType type;
    private LocalDateTime scheduledAt;
    private int retryCount;
    private String errorMessage;
    
    public enum EmailType {
        ORDER_CONFIRMATION,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        REFUND_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        GENERAL_NOTIFICATION
    }
}

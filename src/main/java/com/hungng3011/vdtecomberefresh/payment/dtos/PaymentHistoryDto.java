package com.hungng3011.vdtecomberefresh.payment.dtos;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {
    private Long id;
    private String orderId;
    private String userId;
    private String gatewayTransactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime paymentDate;
    private String errorCode;
    private String errorMessage;
    private BigDecimal refundAmount;
    private LocalDateTime refundDate;
    private String refundTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

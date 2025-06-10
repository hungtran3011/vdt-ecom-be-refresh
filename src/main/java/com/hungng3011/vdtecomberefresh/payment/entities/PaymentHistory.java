package com.hungng3011.vdtecomberefresh.payment.entities;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history", indexes = {
        @Index(name = "idx_payment_history_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_history_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_history_status", columnList = "status"),
        @Index(name = "idx_payment_history_payment_date", columnList = "payment_date"),
        @Index(name = "idx_payment_history_gateway_transaction_id", columnList = "gateway_transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId; // Transaction ID from payment gateway (e.g., Viettel)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column
    private String currency = "VND";
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "gateway_response")
    @Lob
    private String gatewayResponse; // Store raw gateway response for debugging
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "refund_amount", precision = 19, scale = 4)
    private BigDecimal refundAmount;
    
    @Column(name = "refund_date")
    private LocalDateTime refundDate;
    
    @Column(name = "refund_transaction_id")
    private String refundTransactionId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

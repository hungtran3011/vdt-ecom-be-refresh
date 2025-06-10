package com.hungng3011.vdtecomberefresh.payment.dtos.filters;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for filtering payment-related data from orders
 * with comprehensive search criteria and SQL injection protection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterDto {
    
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;
    
    @Size(max = 100, message = "Order ID must not exceed 100 characters")
    private String orderId;
    
    @Size(max = 100, message = "Payment ID must not exceed 100 characters")
    private String paymentId;
    
    private List<PaymentStatus> paymentStatuses;
    
    private List<PaymentMethod> paymentMethods;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum amount must be non-negative")
    private BigDecimal minAmount;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum amount must be non-negative")
    private BigDecimal maxAmount;
    
    @Past(message = "Start date must be in the past")
    private LocalDateTime paymentDateAfter;
    
    @Past(message = "End date must be in the past")
    private LocalDateTime paymentDateBefore;
    
    @Past(message = "Start date must be in the past")
    private LocalDateTime orderCreatedAfter;
    
    @Past(message = "End date must be in the past")
    private LocalDateTime orderCreatedBefore;
    
    // Transaction-specific filtering
    @Size(max = 50, message = "Transaction status must not exceed 50 characters")
    private String transactionStatus;
    
    @Size(max = 100, message = "Error code must not exceed 100 characters")
    private String errorCode;
    
    // Aggregate filtering for business intelligence
    private Boolean isRefunded;
    private Boolean hasFailedAttempts;
    
    // Date range presets for common queries
    private DateRangePreset dateRangePreset;
    
    // Sorting options
    @Builder.Default
    private PaymentSortField sortBy = PaymentSortField.PAYMENT_DATE;
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;
    
    // Pagination
    @Min(value = 0, message = "Page must be non-negative")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "Size must be positive")
    @Builder.Default
    private Integer size = 20;
    
    public enum PaymentSortField {
        PAYMENT_DATE, AMOUNT, ORDER_ID, PAYMENT_ID, USER_ID
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    public enum DateRangePreset {
        TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, THIS_YEAR
    }
}

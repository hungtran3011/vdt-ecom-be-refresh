package com.hungng3011.vdtecomberefresh.order.dtos.filters;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
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
 * DTO for filtering orders with comprehensive search criteria
 * and SQL injection protection through validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterDto {
    
    @Size(max = 100, message = "User email must not exceed 100 characters")
    private String userEmail;
    
    private List<OrderStatus> orderStatuses;
    
    private List<PaymentStatus> paymentStatuses;
    
    private List<PaymentMethod> paymentMethods;
    
    @Size(max = 100, message = "Phone must not exceed 100 characters")
    private String phone;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum total price must be non-negative")
    private BigDecimal minTotalPrice;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum total price must be non-negative")
    private BigDecimal maxTotalPrice;
    
    @Past(message = "Start date must be in the past")
    private LocalDateTime createdAfter;
    
    @Past(message = "End date must be in the past")
    private LocalDateTime createdBefore;
    
    private LocalDateTime updatedAfter;
    
    private LocalDateTime updatedBefore;
    
    @Size(max = 100, message = "Payment ID must not exceed 100 characters")
    private String paymentId;
    
    // Product-based filtering
    @Min(value = 1, message = "Product ID must be positive")
    private Long productId;
    
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String productName;
    
    // Sorting options
    @Builder.Default
    private OrderSortField sortBy = OrderSortField.CREATED_AT;
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;
    
    // Pagination
    @Min(value = 0, message = "Page must be non-negative")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "Size must be positive")
    @Builder.Default
    private Integer size = 20;
    
    public enum OrderSortField {
        ID, CREATED_AT, UPDATED_AT, TOTAL_PRICE, USER_ID
    }
    
    public enum SortDirection {
        ASC, DESC
    }
}

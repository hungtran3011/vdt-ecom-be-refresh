package com.hungng3011.vdtecomberefresh.payment.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.payment.dtos.filters.PaymentFilterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for secure payment filtering with SQL injection protection
 * Uses parameterized queries and input validation to prevent security vulnerabilities
 * 
 * Since payments are tracked through Order entities, this service filters orders
 * with payment-related criteria and returns order data with payment context
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFilterService {
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    /**
     * Filter payment data (through orders) using comprehensive criteria with SQL injection protection
     * 
     * @param filterDto Filter criteria with validated input
     * @return Paginated and filtered payment results as order DTOs
     */
    public PagedResponse<OrderDto> filterPayments(PaymentFilterDto filterDto) {
        log.info("Filtering payments with criteria: {}", filterDto);
        
        try {
            // Validate and sanitize input
            PaymentFilterDto sanitizedFilter = sanitizeFilterDto(filterDto);
            
            // Handle date range presets
            applyDateRangePreset(sanitizedFilter);
            
            // Create pageable with secure sorting
            Pageable pageable = createSecurePageable(sanitizedFilter);
            
            Page<Order> orderPage;
            
            // Apply payment-focused filtering
            orderPage = applyPaymentFilter(sanitizedFilter, pageable);
            
            // Convert to DTOs
            List<OrderDto> orderDtos = orderPage.getContent()
                    .stream()
                    .map(orderMapper::toDto)
                    .collect(Collectors.toList());
            
            // Build response with pagination metadata
            return buildPagedResponse(orderDtos, orderPage, sanitizedFilter);
            
        } catch (Exception e) {
            log.error("Error filtering payments with criteria: {}", filterDto, e);
            throw new RuntimeException("Failed to filter payments", e);
        }
    }
    
    /**
     * Get payment statistics for business intelligence
     */
    public PaymentStatisticsDto getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting payment statistics from {} to {}", startDate, endDate);
        
        try {
            Object[] stats = orderRepository.getPaymentStatistics(startDate, endDate);
            
            if (stats != null && stats.length >= 5) {
                return PaymentStatisticsDto.builder()
                        .totalPayments(((Number) stats[0]).longValue())
                        .successfulPayments(((Number) stats[1]).longValue())
                        .failedPayments(((Number) stats[2]).longValue())
                        .refundedPayments(((Number) stats[3]).longValue())
                        .totalRevenue((BigDecimal) stats[4])
                        .startDate(startDate)
                        .endDate(endDate)
                        .build();
            }
            
            return PaymentStatisticsDto.builder()
                    .totalPayments(0L)
                    .successfulPayments(0L)
                    .failedPayments(0L)
                    .refundedPayments(0L)
                    .totalRevenue(BigDecimal.ZERO)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting payment statistics from {} to {}", startDate, endDate, e);
            throw new RuntimeException("Failed to get payment statistics", e);
        }
    }
    
    /**
     * Sanitize filter input to prevent injection attacks
     */
    private PaymentFilterDto sanitizeFilterDto(PaymentFilterDto filterDto) {
        if (filterDto == null) {
            return new PaymentFilterDto();
        }
        
        PaymentFilterDto sanitized = new PaymentFilterDto();
        
        // Sanitize string fields
        sanitized.setUserId(sanitizeStringInput(filterDto.getUserId()));
        sanitized.setOrderId(sanitizeStringInput(filterDto.getOrderId()));
        sanitized.setPaymentId(sanitizeStringInput(filterDto.getPaymentId()));
        sanitized.setTransactionStatus(sanitizeStringInput(filterDto.getTransactionStatus()));
        sanitized.setErrorCode(sanitizeStringInput(filterDto.getErrorCode()));
        
        // Copy validated enum and numeric fields
        sanitized.setPaymentStatuses(filterDto.getPaymentStatuses());
        sanitized.setPaymentMethods(filterDto.getPaymentMethods());
        sanitized.setMinAmount(filterDto.getMinAmount());
        sanitized.setMaxAmount(filterDto.getMaxAmount());
        sanitized.setIsRefunded(filterDto.getIsRefunded());
        sanitized.setHasFailedAttempts(filterDto.getHasFailedAttempts());
        
        // Validate and copy date fields
        sanitized.setPaymentDateAfter(validateDate(filterDto.getPaymentDateAfter()));
        sanitized.setPaymentDateBefore(validateDate(filterDto.getPaymentDateBefore()));
        sanitized.setOrderCreatedAfter(validateDate(filterDto.getOrderCreatedAfter()));
        sanitized.setOrderCreatedBefore(validateDate(filterDto.getOrderCreatedBefore()));
        
        // Copy date range preset
        sanitized.setDateRangePreset(filterDto.getDateRangePreset());
        
        // Copy sorting and pagination
        sanitized.setSortBy(filterDto.getSortBy() != null ? filterDto.getSortBy() : PaymentFilterDto.PaymentSortField.PAYMENT_DATE);
        sanitized.setSortDirection(filterDto.getSortDirection() != null ? filterDto.getSortDirection() : PaymentFilterDto.SortDirection.DESC);
        sanitized.setPage(filterDto.getPage() != null ? Math.max(0, filterDto.getPage()) : 0);
        sanitized.setSize(filterDto.getSize() != null ? Math.min(100, Math.max(1, filterDto.getSize())) : 20);
        
        return sanitized;
    }
    
    /**
     * Sanitize string input to remove potentially dangerous characters
     */
    private String sanitizeStringInput(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        
        // Remove potentially dangerous SQL injection patterns
        String sanitized = input.trim()
                .replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", "")
                .replaceAll("[';\"\\\\]", ""); // Remove quotes and backslashes
        
        return sanitized.length() > 0 ? sanitized : null;
    }
    
    /**
     * Validate date to ensure it's reasonable
     */
    private LocalDateTime validateDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        
        // Ensure date is not too far in the future or past
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minDate = now.minusYears(10);
        LocalDateTime maxDate = now.plusDays(1);
        
        if (date.isBefore(minDate) || date.isAfter(maxDate)) {
            return null;
        }
        
        return date;
    }
    
    /**
     * Apply date range preset if specified
     */
    private void applyDateRangePreset(PaymentFilterDto filterDto) {
        if (filterDto.getDateRangePreset() == null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate = now;
        
        switch (filterDto.getDateRangePreset()) {
            case TODAY:
                startDate = now.toLocalDate().atStartOfDay();
                break;
            case YESTERDAY:
                startDate = now.minusDays(1).toLocalDate().atStartOfDay();
                endDate = now.toLocalDate().atStartOfDay();
                break;
            case LAST_7_DAYS:
                startDate = now.minusDays(7).toLocalDate().atStartOfDay();
                break;
            case LAST_30_DAYS:
                startDate = now.minusDays(30).toLocalDate().atStartOfDay();
                break;
            case THIS_MONTH:
                startDate = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                break;
            case LAST_MONTH:
                startDate = now.minusMonths(1).toLocalDate().withDayOfMonth(1).atStartOfDay();
                endDate = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                break;
            case THIS_YEAR:
                startDate = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                break;
            default:
                return;
        }
        
        // Only set if not already specified
        if (filterDto.getPaymentDateAfter() == null) {
            filterDto.setPaymentDateAfter(startDate);
        }
        if (filterDto.getPaymentDateBefore() == null) {
            filterDto.setPaymentDateBefore(endDate);
        }
    }
    
    /**
     * Create secure pageable with sorting validation
     */
    private Pageable createSecurePageable(PaymentFilterDto filterDto) {
        String sortField = mapSortField(filterDto.getSortBy());
        Sort.Direction direction = filterDto.getSortDirection() == PaymentFilterDto.SortDirection.DESC 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        
        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(filterDto.getPage(), filterDto.getSize(), sort);
    }
    
    /**
     * Map sort field enum to actual entity field names - prevents field injection
     */
    private String mapSortField(PaymentFilterDto.PaymentSortField sortField) {
        return switch (sortField) {
            case PAYMENT_DATE -> "updatedAt"; // Use updatedAt as proxy for payment date
            case AMOUNT -> "totalPrice";
            case ORDER_ID -> "id";
            case PAYMENT_ID -> "paymentId";
            case USER_ID -> "userId";
            default -> "updatedAt";
        };
    }
    
    /**
     * Apply payment-focused filter using order repository
     */
    private Page<Order> applyPaymentFilter(PaymentFilterDto filterDto, Pageable pageable) {
        return orderRepository.findByPaymentCriteria(
                filterDto.getUserId(),
                filterDto.getOrderId(),
                filterDto.getPaymentId(),
                filterDto.getPaymentStatuses(),
                filterDto.getPaymentMethods(),
                filterDto.getMinAmount(),
                filterDto.getMaxAmount(),
                filterDto.getPaymentDateAfter(),
                filterDto.getPaymentDateBefore(),
                filterDto.getOrderCreatedAfter(),
                filterDto.getOrderCreatedBefore(),
                pageable);
    }
    
    /**
     * Build paged response with metadata
     */
    private PagedResponse<OrderDto> buildPagedResponse(List<OrderDto> content, Page<Order> page, PaymentFilterDto filterDto) {
        PagedResponse.PaginationMetadata metadata = PagedResponse.PaginationMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
        
        return new PagedResponse<>(content, metadata);
    }
    
    /**
     * DTO for payment statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentStatisticsDto {
        private Long totalPayments;
        private Long successfulPayments;
        private Long failedPayments;
        private Long refundedPayments;
        private BigDecimal totalRevenue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
}

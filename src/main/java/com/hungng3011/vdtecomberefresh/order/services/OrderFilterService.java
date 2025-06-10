package com.hungng3011.vdtecomberefresh.order.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.dtos.filters.OrderFilterDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for secure order filtering with SQL injection protection
 * Uses parameterized queries and input validation to prevent security vulnerabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFilterService {
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    /**
     * Filter orders using comprehensive criteria with SQL injection protection
     * 
     * @param filterDto Filter criteria with validated input
     * @return Paginated and filtered order results
     */
    public PagedResponse<OrderDto> filterOrders(OrderFilterDto filterDto) {
        log.info("Filtering orders with criteria: {}", filterDto);
        
        try {
            // Validate and sanitize input
            OrderFilterDto sanitizedFilter = sanitizeFilterDto(filterDto);
            
            // Create pageable with secure sorting
            Pageable pageable = createSecurePageable(sanitizedFilter);
            
            Page<Order> orderPage;
            
            // Apply filtering based on criteria complexity
            if (hasProductCriteria(sanitizedFilter)) {
                orderPage = applyProductFilter(sanitizedFilter, pageable);
            } else if (hasComprehensiveCriteria(sanitizedFilter)) {
                orderPage = applyComprehensiveFilter(sanitizedFilter, pageable);
            } else if (hasBasicCriteria(sanitizedFilter)) {
                orderPage = applyBasicFilter(sanitizedFilter, pageable);
            } else {
                // Default: return all orders with pagination
                orderPage = orderRepository.findAll(pageable);
            }
            
            // Convert to DTOs
            List<OrderDto> orderDtos = orderPage.getContent()
                    .stream()
                    .map(orderMapper::toDto)
                    .collect(Collectors.toList());
            
            // Build response with pagination metadata
            return buildPagedResponse(orderDtos, orderPage, sanitizedFilter);
            
        } catch (Exception e) {
            log.error("Error filtering orders with criteria: {}", filterDto, e);
            throw new RuntimeException("Failed to filter orders", e);
        }
    }
    
    /**
     * Get order statistics for business intelligence
     */
    public OrderStatisticsDto getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting order statistics from {} to {}", startDate, endDate);
        
        try {
            Object[] stats = orderRepository.getPaymentStatistics(startDate, endDate);
            
            if (stats != null && stats.length >= 5) {
                return OrderStatisticsDto.builder()
                        .totalOrders(((Number) stats[0]).longValue())
                        .successfulPayments(((Number) stats[1]).longValue())
                        .failedPayments(((Number) stats[2]).longValue())
                        .refundedPayments(((Number) stats[3]).longValue())
                        .totalRevenue((java.math.BigDecimal) stats[4])
                        .startDate(startDate)
                        .endDate(endDate)
                        .build();
            }
            
            return OrderStatisticsDto.builder()
                    .totalOrders(0L)
                    .successfulPayments(0L)
                    .failedPayments(0L)
                    .refundedPayments(0L)
                    .totalRevenue(java.math.BigDecimal.ZERO)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting order statistics from {} to {}", startDate, endDate, e);
            throw new RuntimeException("Failed to get order statistics", e);
        }
    }
    
    /**
     * Sanitize filter input to prevent injection attacks
     */
    private OrderFilterDto sanitizeFilterDto(OrderFilterDto filterDto) {
        if (filterDto == null) {
            return new OrderFilterDto();
        }
        
        OrderFilterDto sanitized = new OrderFilterDto();
        
        // Sanitize string fields
        sanitized.setUserEmail(sanitizeStringInput(filterDto.getUserEmail()));
        sanitized.setPhone(sanitizeStringInput(filterDto.getPhone()));
        sanitized.setAddress(sanitizeStringInput(filterDto.getAddress()));
        sanitized.setPaymentId(sanitizeStringInput(filterDto.getPaymentId()));
        sanitized.setProductName(sanitizeStringInput(filterDto.getProductName()));
        
        // Copy validated enum and numeric fields
        sanitized.setOrderStatuses(filterDto.getOrderStatuses());
        sanitized.setPaymentStatuses(filterDto.getPaymentStatuses());
        sanitized.setPaymentMethods(filterDto.getPaymentMethods());
        sanitized.setMinTotalPrice(filterDto.getMinTotalPrice());
        sanitized.setMaxTotalPrice(filterDto.getMaxTotalPrice());
        sanitized.setProductId(filterDto.getProductId());
        
        // Validate and copy date fields
        sanitized.setCreatedAfter(validateDate(filterDto.getCreatedAfter()));
        sanitized.setCreatedBefore(validateDate(filterDto.getCreatedBefore()));
        sanitized.setUpdatedAfter(validateDate(filterDto.getUpdatedAfter()));
        sanitized.setUpdatedBefore(validateDate(filterDto.getUpdatedBefore()));
        
        // Copy sorting and pagination
        sanitized.setSortBy(filterDto.getSortBy() != null ? filterDto.getSortBy() : OrderFilterDto.OrderSortField.CREATED_AT);
        sanitized.setSortDirection(filterDto.getSortDirection() != null ? filterDto.getSortDirection() : OrderFilterDto.SortDirection.DESC);
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
     * Create secure pageable with sorting validation
     */
    private Pageable createSecurePageable(OrderFilterDto filterDto) {
        String sortField = mapSortField(filterDto.getSortBy());
        Sort.Direction direction = filterDto.getSortDirection() == OrderFilterDto.SortDirection.DESC 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        
        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(filterDto.getPage(), filterDto.getSize(), sort);
    }
    
    /**
     * Map sort field enum to actual entity field names - prevents field injection
     */
    private String mapSortField(OrderFilterDto.OrderSortField sortField) {
        return switch (sortField) {
            case ID -> "id";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case TOTAL_PRICE -> "totalPrice";
            case USER_ID -> "userEmail";
            default -> "createdAt";
        };
    }
    
    /**
     * Check if filter has product-related criteria
     */
    private boolean hasProductCriteria(OrderFilterDto filterDto) {
        return filterDto.getProductId() != null || StringUtils.hasText(filterDto.getProductName());
    }
    
    /**
     * Check if filter has comprehensive criteria
     */
    private boolean hasComprehensiveCriteria(OrderFilterDto filterDto) {
        return (filterDto.getOrderStatuses() != null && !filterDto.getOrderStatuses().isEmpty()) ||
               (filterDto.getPaymentStatuses() != null && !filterDto.getPaymentStatuses().isEmpty()) ||
               (filterDto.getPaymentMethods() != null && !filterDto.getPaymentMethods().isEmpty()) ||
               filterDto.getMinTotalPrice() != null ||
               filterDto.getMaxTotalPrice() != null ||
               filterDto.getCreatedAfter() != null ||
               filterDto.getCreatedBefore() != null;
    }
    
    /**
     * Check if filter has basic criteria
     */
    private boolean hasBasicCriteria(OrderFilterDto filterDto) {
        return StringUtils.hasText(filterDto.getUserEmail()) ||
               StringUtils.hasText(filterDto.getPhone()) ||
               StringUtils.hasText(filterDto.getAddress()) ||
               StringUtils.hasText(filterDto.getPaymentId());
    }
    
    /**
     * Apply product-based filter
     */
    private Page<Order> applyProductFilter(OrderFilterDto filterDto, Pageable pageable) {
        return orderRepository.findByProductCriteria(
                filterDto.getUserEmail(),
                filterDto.getProductId(),
                filterDto.getProductName(),
                filterDto.getOrderStatuses(),
                filterDto.getPaymentStatuses(),
                pageable);
    }
    
    /**
     * Apply comprehensive filter
     */
    private Page<Order> applyComprehensiveFilter(OrderFilterDto filterDto, Pageable pageable) {
        return orderRepository.findByMultipleCriteria(
                filterDto.getUserEmail(),
                filterDto.getOrderStatuses(),
                filterDto.getPaymentStatuses(),
                filterDto.getPaymentMethods(),
                filterDto.getPhone(),
                filterDto.getAddress(),
                filterDto.getMinTotalPrice(),
                filterDto.getMaxTotalPrice(),
                filterDto.getCreatedAfter(),
                filterDto.getCreatedBefore(),
                filterDto.getUpdatedAfter(),
                filterDto.getUpdatedBefore(),
                filterDto.getPaymentId(),
                pageable);
    }
    
    /**
     * Apply basic filter for simple criteria
     */
    private Page<Order> applyBasicFilter(OrderFilterDto filterDto, Pageable pageable) {
        if (StringUtils.hasText(filterDto.getUserEmail())) {
            return orderRepository.findByUserEmail(filterDto.getUserEmail(), pageable);
        }
        
        if (filterDto.getOrderStatuses() != null && !filterDto.getOrderStatuses().isEmpty()) {
            return orderRepository.findByStatusIn(filterDto.getOrderStatuses(), pageable);
        }
        
        if (filterDto.getPaymentStatuses() != null && !filterDto.getPaymentStatuses().isEmpty()) {
            return orderRepository.findByPaymentStatusIn(filterDto.getPaymentStatuses(), pageable);
        }
        
        // Fallback to comprehensive filter
        return applyComprehensiveFilter(filterDto, pageable);
    }
    
    /**
     * Build paged response with metadata
     */
    private PagedResponse<OrderDto> buildPagedResponse(List<OrderDto> content, Page<Order> page, OrderFilterDto filterDto) {
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
     * DTO for order statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderStatisticsDto {
        private Long totalOrders;
        private Long successfulPayments;
        private Long failedPayments;
        private Long refundedPayments;
        private java.math.BigDecimal totalRevenue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
}

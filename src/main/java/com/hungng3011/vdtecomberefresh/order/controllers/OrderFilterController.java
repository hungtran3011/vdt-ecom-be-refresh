package com.hungng3011.vdtecomberefresh.order.controllers;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.dtos.filters.OrderFilterDto;
import com.hungng3011.vdtecomberefresh.order.services.OrderFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for order filtering operations with comprehensive security protection.
 * Provides secure filtering, searching, and statistical endpoints for orders.
 */
@RestController
@RequestMapping("/v1/orders/filter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Filter", description = "Secure order filtering and search operations")
public class OrderFilterController {
    
    private final OrderFilterService orderFilterService;

    /**
     * Filter orders with advanced criteria and security protection
     */
    @PostMapping("/search")
    @Operation(
        summary = "Filter orders with advanced criteria",
        description = "Secure filtering with SQL injection protection, input validation, and pagination"
    )
    public ResponseEntity<PagedResponse<OrderDto>> filterOrders(
            @Valid @RequestBody OrderFilterDto filterDto) {
        
        log.info("Filtering orders with criteria: userEmail={}, orderStatuses={}, dateRange=[{} to {}], page={}, size={}", 
                filterDto.getUserEmail(), filterDto.getOrderStatuses(), 
                filterDto.getCreatedAfter(), filterDto.getCreatedBefore(),
                filterDto.getPage(), filterDto.getSize());
        
        try {
            PagedResponse<OrderDto> response = orderFilterService.filterOrders(filterDto);
            
            log.info("Successfully filtered orders - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for orders: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error filtering orders with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Get order statistics based on date range
     */
    @PostMapping("/statistics")
    @Operation(
        summary = "Get order statistics",
        description = "Retrieve statistical information about orders within date range"
    )
    public ResponseEntity<Map<String, Object>> getOrderStatistics(
            @Valid @RequestBody OrderFilterDto filterDto) {
        
        log.info("Getting order statistics with criteria: userEmail={}, orderStatuses={}, dateRange=[{} to {}]", 
                filterDto.getUserEmail(), filterDto.getOrderStatuses(), 
                filterDto.getCreatedAfter(), filterDto.getCreatedBefore());
        
        try {
            Map<String, Object> statistics = new java.util.HashMap<>();
            
            if (filterDto.getCreatedAfter() != null && filterDto.getCreatedBefore() != null) {
                var orderStats = orderFilterService.getOrderStatistics(filterDto.getCreatedAfter(), filterDto.getCreatedBefore());
                statistics.put("orderStatistics", orderStats);
                statistics.put("hasDateRange", true);
            } else {
                statistics.put("message", "Date range required for detailed statistics");
                statistics.put("hasDateRange", false);
            }
            
            statistics.put("filterCriteria", Map.of(
                "hasUserFilter", filterDto.getUserEmail() != null,
                "hasStatusFilter", filterDto.getOrderStatuses() != null && !filterDto.getOrderStatuses().isEmpty(),
                "hasDateFilter", filterDto.getCreatedAfter() != null || filterDto.getCreatedBefore() != null
            ));
            
            log.info("Successfully retrieved order statistics");
            
            return ResponseEntity.ok(statistics);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for order statistics: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting order statistics with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Get orders by user with filtering
     */
    @GetMapping("/user/{userEmail}")
    @Operation(
        summary = "Get orders by user with filtering",
        description = "Retrieve orders for specific user with additional filtering options"
    )
    public ResponseEntity<PagedResponse<OrderDto>> getOrdersByUser(
            @Parameter(description = "User Email")
            @PathVariable String userEmail,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "CREATED_AT") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Getting orders by user {} with page={}, size={}", userEmail, page, size);
        
        try {
            OrderFilterDto filterDto = OrderFilterDto.builder()
                    .userEmail(userEmail)
                    .page(page)
                    .size(size)
                    .sortBy(mapSortField(sortBy))
                    .sortDirection(mapSortDirection(sortDir))
                    .build();
            
            PagedResponse<OrderDto> response = orderFilterService.filterOrders(filterDto);
            
            log.info("Successfully retrieved orders by user - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for user orders: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting orders by user {}: {}", userEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * Health check endpoint for order filter service
     */
    @GetMapping("/health")
    @Operation(summary = "Order filter service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "order-filter",
            "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Map string to OrderSortField enum safely
     */
    private OrderFilterDto.OrderSortField mapSortField(String sortBy) {
        if (sortBy == null) return OrderFilterDto.OrderSortField.CREATED_AT;
        
        try {
            return OrderFilterDto.OrderSortField.valueOf(sortBy.toUpperCase());
        } catch (Exception e) {
            return OrderFilterDto.OrderSortField.CREATED_AT;
        }
    }
    
    /**
     * Map string to SortDirection enum safely
     */
    private OrderFilterDto.SortDirection mapSortDirection(String sortDir) {
        if (sortDir == null) return OrderFilterDto.SortDirection.DESC;
        
        try {
            return OrderFilterDto.SortDirection.valueOf(sortDir.toUpperCase());
        } catch (Exception e) {
            return OrderFilterDto.SortDirection.DESC;
        }
    }
}

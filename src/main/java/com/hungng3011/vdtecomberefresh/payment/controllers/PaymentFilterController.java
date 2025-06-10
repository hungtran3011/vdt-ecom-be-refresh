package com.hungng3011.vdtecomberefresh.payment.controllers;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.payment.dtos.filters.PaymentFilterDto;
import com.hungng3011.vdtecomberefresh.payment.services.PaymentFilterService;
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
 * REST controller for payment filtering operations with comprehensive security protection.
 * Provides secure filtering, searching, and statistical endpoints for payment data.
 */
@RestController
@RequestMapping("/v1/payments/filter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Filter", description = "Secure payment filtering and search operations")
public class PaymentFilterController {
    
    private final PaymentFilterService paymentFilterService;

    /**
     * Filter payment data with advanced criteria and security protection
     */
    @PostMapping("/search")
    @Operation(
        summary = "Filter payment data with advanced criteria",
        description = "Secure filtering with SQL injection protection, input validation, and pagination"
    )
    public ResponseEntity<PagedResponse<OrderDto>> filterPayments(
            @Valid @RequestBody PaymentFilterDto filterDto) {
        
        log.info("Filtering payments with criteria: userId={}, paymentStatuses={}, paymentMethods={}, amountRange=[{}-{}], page={}, size={}", 
                filterDto.getUserId(), filterDto.getPaymentStatuses(), filterDto.getPaymentMethods(),
                filterDto.getMinAmount(), filterDto.getMaxAmount(),
                filterDto.getPage(), filterDto.getSize());
        
        try {
            PagedResponse<OrderDto> response = paymentFilterService.filterPayments(filterDto);
            
            log.info("Successfully filtered payments - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for payments: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error filtering payments with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Get payment statistics based on date range
     */
    @PostMapping("/statistics")
    @Operation(
        summary = "Get payment statistics",
        description = "Retrieve statistical information about payments within date range"
    )
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(
            @Valid @RequestBody PaymentFilterDto filterDto) {
        
        log.info("Getting payment statistics with criteria: userId={}, paymentStatuses={}, dateRange=[{} to {}]", 
                filterDto.getUserId(), filterDto.getPaymentStatuses(), 
                filterDto.getPaymentDateAfter(), filterDto.getPaymentDateBefore());
        
        try {
            Map<String, Object> statistics = new java.util.HashMap<>();
            
            if (filterDto.getPaymentDateAfter() != null && filterDto.getPaymentDateBefore() != null) {
                var paymentStats = paymentFilterService.getPaymentStatistics(filterDto.getPaymentDateAfter(), filterDto.getPaymentDateBefore());
                statistics.put("paymentStatistics", paymentStats);
                statistics.put("hasDateRange", true);
            } else {
                statistics.put("message", "Date range required for detailed statistics");
                statistics.put("hasDateRange", false);
            }
            
            statistics.put("filterCriteria", Map.of(
                "hasUserFilter", filterDto.getUserId() != null,
                "hasPaymentStatusFilter", filterDto.getPaymentStatuses() != null && !filterDto.getPaymentStatuses().isEmpty(),
                "hasPaymentMethodFilter", filterDto.getPaymentMethods() != null && !filterDto.getPaymentMethods().isEmpty(),
                "hasAmountFilter", filterDto.getMinAmount() != null || filterDto.getMaxAmount() != null,
                "hasDateFilter", filterDto.getPaymentDateAfter() != null || filterDto.getPaymentDateBefore() != null
            ));
            
            log.info("Successfully retrieved payment statistics");
            
            return ResponseEntity.ok(statistics);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for payment statistics: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting payment statistics with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Get payments by user with filtering
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get payments by user with filtering",
        description = "Retrieve payments for specific user with additional filtering options"
    )
    public ResponseEntity<PagedResponse<OrderDto>> getPaymentsByUser(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "PAYMENT_DATE") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Getting payments by user {} with page={}, size={}", userId, page, size);
        
        try {
            PaymentFilterDto filterDto = PaymentFilterDto.builder()
                    .userId(userId)
                    .page(page)
                    .size(size)
                    .sortBy(mapSortField(sortBy))
                    .sortDirection(mapSortDirection(sortDir))
                    .build();
            
            PagedResponse<OrderDto> response = paymentFilterService.filterPayments(filterDto);
            
            log.info("Successfully retrieved payments by user - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for user payments: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting payments by user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get payments with date range presets
     */
    @GetMapping("/preset/{preset}")
    @Operation(
        summary = "Get payments with date range presets",
        description = "Retrieve payments using predefined date ranges (TODAY, YESTERDAY, LAST_7_DAYS, etc.)"
    )
    public ResponseEntity<PagedResponse<OrderDto>> getPaymentsByDatePreset(
            @Parameter(description = "Date range preset")
            @PathVariable String preset,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "PAYMENT_DATE") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Getting payments by date preset {} with page={}, size={}", preset, page, size);
        
        try {
            PaymentFilterDto filterDto = PaymentFilterDto.builder()
                    .dateRangePreset(mapDateRangePreset(preset))
                    .page(page)
                    .size(size)
                    .sortBy(mapSortField(sortBy))
                    .sortDirection(mapSortDirection(sortDir))
                    .build();
            
            PagedResponse<OrderDto> response = paymentFilterService.filterPayments(filterDto);
            
            log.info("Successfully retrieved payments by preset - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid date preset for payments: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting payments by preset {}: {}", preset, e.getMessage());
            throw e;
        }
    }

    /**
     * Health check endpoint for payment filter service
     */
    @GetMapping("/health")
    @Operation(summary = "Payment filter service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "payment-filter",
            "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Map string to PaymentSortField enum safely
     */
    private PaymentFilterDto.PaymentSortField mapSortField(String sortBy) {
        if (sortBy == null) return PaymentFilterDto.PaymentSortField.PAYMENT_DATE;
        
        try {
            return PaymentFilterDto.PaymentSortField.valueOf(sortBy.toUpperCase());
        } catch (Exception e) {
            return PaymentFilterDto.PaymentSortField.PAYMENT_DATE;
        }
    }
    
    /**
     * Map string to SortDirection enum safely
     */
    private PaymentFilterDto.SortDirection mapSortDirection(String sortDir) {
        if (sortDir == null) return PaymentFilterDto.SortDirection.DESC;
        
        try {
            return PaymentFilterDto.SortDirection.valueOf(sortDir.toUpperCase());
        } catch (Exception e) {
            return PaymentFilterDto.SortDirection.DESC;
        }
    }
    
    /**
     * Map string to DateRangePreset enum safely
     */
    private PaymentFilterDto.DateRangePreset mapDateRangePreset(String preset) {
        if (preset == null) return PaymentFilterDto.DateRangePreset.LAST_7_DAYS;
        
        try {
            return PaymentFilterDto.DateRangePreset.valueOf(preset.toUpperCase());
        } catch (Exception e) {
            return PaymentFilterDto.DateRangePreset.LAST_7_DAYS;
        }
    }
}

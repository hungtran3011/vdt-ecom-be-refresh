package com.hungng3011.vdtecomberefresh.stats.controllers;

import com.hungng3011.vdtecomberefresh.stats.dtos.SystemStatsDto;
import com.hungng3011.vdtecomberefresh.stats.services.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for system statistics and analytics.
 * Provides comprehensive system metrics for administrators and monitoring.
 */
@RestController
@RequestMapping("/v1/stats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "System statistics and analytics operations")
@SecurityRequirement(name = "bearer-jwt")
public class StatsController {

    private final StatsService statsService;

    /**
     * Get comprehensive system statistics (ADMIN only)
     * Returns detailed metrics about products, orders, carts, payments, users, and performance
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get system statistics", description = "Admin operation to retrieve comprehensive system statistics and metrics")
    public ResponseEntity<SystemStatsDto> getSystemStats() {
        log.info("Fetching comprehensive system statistics");
        try {
            SystemStatsDto stats = statsService.getSystemStats();
            log.info("Successfully generated system statistics at: {}", stats.getGeneratedAt());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error generating system statistics", e);
            throw e;
        }
    }

    /**
     * Get product statistics (ADMIN only)
     * Returns metrics about products, categories, and stock
     */
    @GetMapping("/products")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get product statistics", description = "Admin operation to retrieve product-related statistics")
    public ResponseEntity<SystemStatsDto.ProductStatsDto> getProductStats() {
        log.info("Fetching product statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated product statistics");
            return ResponseEntity.ok(systemStats.getProductStats());
        } catch (Exception e) {
            log.error("Error generating product statistics", e);
            throw e;
        }
    }

    /**
     * Get order statistics (ADMIN only)
     * Returns metrics about orders, sales, and revenue
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get order statistics", description = "Admin operation to retrieve order-related statistics")
    public ResponseEntity<SystemStatsDto.OrderStatsDto> getOrderStats() {
        log.info("Fetching order statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated order statistics");
            return ResponseEntity.ok(systemStats.getOrderStats());
        } catch (Exception e) {
            log.error("Error generating order statistics", e);
            throw e;
        }
    }

    /**
     * Get payment statistics (ADMIN only)
     * Returns metrics about payment methods, success rates, and transaction volumes
     */
    @GetMapping("/payments")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get payment statistics", description = "Admin operation to retrieve payment-related statistics")
    public ResponseEntity<SystemStatsDto.PaymentStatsDto> getPaymentStats() {
        log.info("Fetching payment statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated payment statistics");
            return ResponseEntity.ok(systemStats.getPaymentStats());
        } catch (Exception e) {
            log.error("Error generating payment statistics", e);
            throw e;
        }
    }

    /**
     * Get user activity statistics (ADMIN only)
     * Returns metrics about user registrations, profiles, and activity
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get user activity statistics", description = "Admin operation to retrieve user activity statistics")
    public ResponseEntity<SystemStatsDto.UserActivityStatsDto> getUserActivityStats() {
        log.info("Fetching user activity statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated user activity statistics");
            return ResponseEntity.ok(systemStats.getUserActivityStats());
        } catch (Exception e) {
            log.error("Error generating user activity statistics", e);
            throw e;
        }
    }

    /**
     * Get stock statistics (ADMIN only)
     * Returns metrics about inventory levels, low stock items, and stock movements
     */
    @GetMapping("/stock")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get stock statistics", description = "Admin operation to retrieve stock and inventory statistics")
    public ResponseEntity<SystemStatsDto.StockStatsDto> getStockStats() {
        log.info("Fetching stock statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated stock statistics");
            return ResponseEntity.ok(systemStats.getStockStats());
        } catch (Exception e) {
            log.error("Error generating stock statistics", e);
            throw e;
        }
    }

    /**
     * Get cart statistics (ADMIN only)
     * Returns metrics about shopping carts, abandonment rates, and cart values
     */
    @GetMapping("/carts")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get cart statistics", description = "Admin operation to retrieve cart-related statistics")
    public ResponseEntity<SystemStatsDto.CartStatsDto> getCartStats() {
        log.info("Fetching cart statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated cart statistics");
            return ResponseEntity.ok(systemStats.getCartStats());
        } catch (Exception e) {
            log.error("Error generating cart statistics", e);
            throw e;
        }
    }

    /**
     * Get performance statistics (ADMIN only)
     * Returns system performance metrics and KPIs
     */
    @GetMapping("/performance")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get performance statistics", description = "Admin operation to retrieve system performance statistics")
    public ResponseEntity<SystemStatsDto.PerformanceStatsDto> getPerformanceStats() {
        log.info("Fetching performance statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated performance statistics");
            return ResponseEntity.ok(systemStats.getPerformanceStats());
        } catch (Exception e) {
            log.error("Error generating performance statistics", e);
            throw e;
        }
    }

    /**
     * Get category statistics (ADMIN only)
     * Returns metrics about product categories and their performance
     */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get category statistics", description = "Admin operation to retrieve category-related statistics")
    public ResponseEntity<SystemStatsDto.CategoryStatsDto> getCategoryStats() {
        log.info("Fetching category statistics");
        try {
            SystemStatsDto systemStats = statsService.getSystemStats();
            log.info("Successfully generated category statistics");
            return ResponseEntity.ok(systemStats.getCategoryStats());
        } catch (Exception e) {
            log.error("Error generating category statistics", e);
            throw e;
        }
    }

    /**
     * Health check endpoint for statistics service
     */
    @GetMapping("/health")
    @Operation(summary = "Statistics service health check")
    public ResponseEntity<Map<String, String>> health() {
        log.info("Statistics service health check requested");
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "statistics",
            "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get statistics summary (ADMIN only)
     * Returns a lightweight summary of key metrics for dashboard display
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Get statistics summary", description = "Admin operation to retrieve a summary of key system statistics")
    public ResponseEntity<Map<String, Object>> getStatsSummary() {
        log.info("Fetching statistics summary");
        try {
            SystemStatsDto stats = statsService.getSystemStats();
            
            Map<String, Object> summary = Map.of(
                "generatedAt", stats.getGeneratedAt(),
                "systemStatus", stats.getSystemStatus(),
                "totalProducts", stats.getProductStats().getTotalProducts(),
                "totalOrders", stats.getOrderStats().getTotalOrders(),
                "totalRevenue", stats.getOrderStats().getTotalRevenue(),
                "totalUsers", stats.getUserActivityStats().getTotalUsers(),
                "lowStockItems", stats.getStockStats().getLowStockItems(),
                "pendingOrders", stats.getOrderStats().getPendingOrders(),
                "successfulPayments", stats.getPaymentStats().getSuccessfulPayments(),
                "averageOrderValue", stats.getOrderStats().getAverageOrderValue()
            );
            
            log.info("Successfully generated statistics summary");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error generating statistics summary", e);
            throw e;
        }
    }
}

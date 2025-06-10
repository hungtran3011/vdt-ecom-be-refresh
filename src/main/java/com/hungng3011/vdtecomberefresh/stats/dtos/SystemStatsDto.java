package com.hungng3011.vdtecomberefresh.stats.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDto {
    
    // Overall System Stats
    private LocalDateTime generatedAt;
    private String systemStatus;
    
    // Product Statistics
    private ProductStatsDto productStats;
    
    // Stock Statistics
    private StockStatsDto stockStats;
    
    // Order Statistics
    private OrderStatsDto orderStats;
    
    // Cart Statistics
    private CartStatsDto cartStats;
    
    // Category Statistics
    private CategoryStatsDto categoryStats;
    
    // Payment Statistics
    private PaymentStatsDto paymentStats;
    
    // User Activity Statistics
    private UserActivityStatsDto userActivityStats;
    
    // Performance Metrics
    private PerformanceStatsDto performanceStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStatsDto {
        private Long totalProducts;
        private Long activeProducts;
        private Long inactiveProducts;
        private Long productsWithVariations;
        private Long totalVariations;
        private Map<String, Long> productsByCategory;
        private BigDecimal averageProductPrice;
        private BigDecimal totalProductValue;
        private List<TopProductDto> topRatedProducts;
        private List<TopProductDto> mostViewedProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockStatsDto {
        private Long totalStockItems;
        private Long inStockItems;
        private Long lowStockItems;
        private Long outOfStockItems;
        private Long preOrderItems;
        private BigDecimal totalStockValue;
        private BigDecimal averageStockValue;
        private Long totalQuantityInStock;
        private Long distinctProductsInStock;
        private List<LowStockItemDto> lowStockAlerts;
        private Map<String, Long> stockByStatus;
        private Long totalStockTransactions;
        private Map<String, Long> stockActionBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatsDto {
        private Long totalOrders;
        private Long pendingOrders;
        private Long confirmedOrders;
        private Long shippedOrders;
        private Long deliveredOrders;
        private Long cancelledOrders;
        private BigDecimal totalRevenue;
        private BigDecimal averageOrderValue;
        private BigDecimal todayRevenue;
        private BigDecimal weekRevenue;
        private BigDecimal monthRevenue;
        private Map<String, Long> ordersByStatus;
        private Map<String, BigDecimal> revenueByPaymentMethod;
        private List<TopOrderDto> recentOrders;
        private OrderTrendsDto trends;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartStatsDto {
        private Long totalActiveCarts;
        private Long totalAbandonedCarts;
        private Long totalCartItems;
        private BigDecimal totalCartValue;
        private BigDecimal averageCartValue;
        private BigDecimal abandonmentRate;
        private List<TopCartItemDto> mostAddedToCart;
        private Map<String, Long> cartsByTimeOfDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatsDto {
        private Long totalCategories;
        private Map<String, Long> productsByCategory;
        private Map<String, BigDecimal> revenueByCategory;
        private List<TopCategoryDto> topCategories;
        private Long totalDynamicFields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatsDto {
        private Long totalPayments;
        private Long successfulPayments;
        private Long failedPayments;
        private Long pendingPayments;
        private BigDecimal totalPaymentAmount;
        private BigDecimal successfulPaymentAmount;
        private BigDecimal failedPaymentAmount;
        private BigDecimal successRate;
        private Map<String, Long> paymentsByMethod;
        private Map<String, BigDecimal> amountByMethod;
        private List<PaymentTrendDto> paymentTrends;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityStatsDto {
        private Long totalUsers;
        private Long activeUsers;
        private Long newUsersToday;
        private Long newUsersThisWeek;
        private Long newUsersThisMonth;
        private Map<String, Long> userActivityByHour;
        private List<TopUserDto> topUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceStatsDto {
        private Double averageResponseTime;
        private Long totalApiCalls;
        private Long errorCount;
        private Double errorRate;
        private Map<String, Long> endpointUsage;
    }

    // Helper DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDto {
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Long views;
        private Double rating;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockItemDto {
        private Long stockId;
        private String sku;
        private String productName;
        private Integer currentQuantity;
        private Integer threshold;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopOrderDto {
        private String orderId;
        private String userId;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCartItemDto {
        private Long productId;
        private String productName;
        private Long addedCount;
        private BigDecimal totalValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategoryDto {
        private Long categoryId;
        private String categoryName;
        private Long productCount;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentTrendDto {
        private String period;
        private Long count;
        private BigDecimal amount;
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUserDto {
        private String userId;
        private Long orderCount;
        private BigDecimal totalSpent;
        private LocalDateTime lastActivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTrendsDto {
        private List<DailyTrendDto> dailyTrends;
        private List<MonthlyTrendDto> monthlyTrends;
        private Map<String, Long> ordersByDayOfWeek;
        private Map<String, Long> ordersByHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendDto {
        private String date;
        private Long orderCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendDto {
        private String month;
        private Long orderCount;
        private BigDecimal revenue;
    }
}

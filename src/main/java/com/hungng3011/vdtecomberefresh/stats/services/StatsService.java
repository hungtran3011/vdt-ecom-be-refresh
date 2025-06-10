package com.hungng3011.vdtecomberefresh.stats.services;

import com.hungng3011.vdtecomberefresh.stats.dtos.SystemStatsDto;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.product.repositories.VariationRepository;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockRepository;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartRepository;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartItemRepository;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryRepository;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryDynamicFieldRepository;
import com.hungng3011.vdtecomberefresh.payment.repositories.PaymentHistoryRepository;
import com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final StockRepository stockRepository;
    // Note: StockHistoryRepository available for future stock analytics features
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryDynamicFieldRepository categoryDynamicFieldRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ProfileRepository profileRepository;
    /**
     * Generate comprehensive system statistics
     */
    public SystemStatsDto getSystemStats() {
        try {
            log.info("Generating comprehensive system statistics");
            
            return SystemStatsDto.builder()
                .generatedAt(LocalDateTime.now())
                .systemStatus("OPERATIONAL")
                .productStats(generateProductStats())
                .stockStats(generateStockStats())
                .orderStats(generateOrderStats())
                .cartStats(generateCartStats())
                .categoryStats(generateCategoryStats())
                .paymentStats(generatePaymentStats())
                .userActivityStats(generateUserActivityStats())
                .performanceStats(generatePerformanceStats())
                .build();
        } catch (Exception e) {
            log.error("Error generating system statistics", e);
            return getErrorStats(e);
        }
    }

    private SystemStatsDto.ProductStatsDto generateProductStats() {
        log.debug("Generating product statistics");
        
        try {
            Long totalProducts = productRepository.count();
            Long totalVariations = variationRepository.count();
            Long productsWithVariations = productRepository.countProductsWithVariations();
            
            BigDecimal averagePrice = productRepository.getAverageProductPrice();
            
            // Products by category
            Map<String, Long> productsByCategory = Collections.emptyMap();
            try {
                productsByCategory = productRepository.countProductsByCategory()
                    .stream()
                    .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]
                    ));
            } catch (Exception e) {
                log.warn("Could not generate products by category", e);
            }

            return SystemStatsDto.ProductStatsDto.builder()
                .totalProducts(totalProducts)
                .activeProducts(totalProducts) // Assuming all products are active
                .inactiveProducts(0L)
                .productsWithVariations(productsWithVariations != null ? productsWithVariations : 0L)
                .totalVariations(totalVariations)
                .productsByCategory(productsByCategory)
                .averageProductPrice(averagePrice != null ? averagePrice : BigDecimal.ZERO)
                .totalProductValue(averagePrice != null && totalProducts > 0 ? 
                    averagePrice.multiply(BigDecimal.valueOf(totalProducts)) : BigDecimal.ZERO)
                .topRatedProducts(Collections.emptyList()) // TODO: Implement when rating system is available
                .mostViewedProducts(Collections.emptyList()) // TODO: Implement when view tracking is available
                .build();
        } catch (Exception e) {
            log.error("Error generating product statistics", e);
            return SystemStatsDto.ProductStatsDto.builder()
                .totalProducts(0L)
                .activeProducts(0L)
                .inactiveProducts(0L)
                .productsWithVariations(0L)
                .totalVariations(0L)
                .productsByCategory(Collections.emptyMap())
                .averageProductPrice(BigDecimal.ZERO)
                .totalProductValue(BigDecimal.ZERO)
                .topRatedProducts(Collections.emptyList())
                .mostViewedProducts(Collections.emptyList())
                .build();
        }
    }

    private SystemStatsDto.StockStatsDto generateStockStats() {
        log.debug("Generating stock statistics");
        
        try {
            Long totalStockItems = stockRepository.count();
            Long inStockItems = stockRepository.countInStockItems();
            Long lowStockItems = stockRepository.countLowStockItems();
            Long outOfStockItems = stockRepository.countOutOfStockItems();
            
            BigDecimal totalStockValue = stockRepository.getTotalStockValue();
            Long totalQuantity = stockRepository.getTotalQuantityInStock();
            Long distinctProducts = stockRepository.countDistinctProductsInStock();
            
            // Low stock alerts
            List<SystemStatsDto.LowStockItemDto> lowStockAlerts = stockRepository.findLowStockItems()
                .stream()
                .limit(10) // Limit to top 10 alerts
                .map(stock -> SystemStatsDto.LowStockItemDto.builder()
                    .stockId(stock.getId())
                    .sku(stock.getSku())
                    .productName(stock.getProduct().getName())
                    .currentQuantity(stock.getQuantity())
                    .threshold(stock.getLowStockThreshold())
                    .status(stock.getStatus().toString())
                    .build())
                .collect(Collectors.toList());
            
            // Stock by status
            List<Object[]> statusCounts = stockRepository.countByStatus();
            Map<String, Long> stockByStatus = statusCounts.stream()
                .collect(Collectors.toMap(
                    result -> result[0].toString(),
                    result -> (Long) result[1]
                ));
            
            return SystemStatsDto.StockStatsDto.builder()
                .totalStockItems(totalStockItems)
                .inStockItems(inStockItems)
                .lowStockItems(lowStockItems)
                .outOfStockItems(outOfStockItems)
                .preOrderItems(0L) // TODO: Add when pre-order status is tracked
                .totalStockValue(totalStockValue != null ? totalStockValue : BigDecimal.ZERO)
                .averageStockValue(totalStockValue != null && totalStockItems > 0 ? 
                    totalStockValue.divide(BigDecimal.valueOf(totalStockItems), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalQuantityInStock(totalQuantity != null ? totalQuantity : 0L)
                .distinctProductsInStock(distinctProducts != null ? distinctProducts : 0L)
                .lowStockAlerts(lowStockAlerts)
                .stockByStatus(stockByStatus)
                .totalStockTransactions(0L) // TODO: Add when stock transaction tracking is available
                .stockActionBreakdown(Collections.emptyMap()) // TODO: Add when stock action tracking is available
                    .build();
        } catch (Exception e) {
            log.error("Error generating stock statistics", e);
            return getErrorStockStats();
        }
    }

    private SystemStatsDto.StockStatsDto getErrorStockStats() {
        return SystemStatsDto.StockStatsDto.builder()
            .totalStockItems(0L)
            .inStockItems(0L)
            .lowStockItems(0L)
            .outOfStockItems(0L)
            .preOrderItems(0L)
            .totalStockValue(BigDecimal.ZERO)
            .averageStockValue(BigDecimal.ZERO)
            .totalQuantityInStock(0L)
            .lowStockAlerts(Collections.emptyList())
            .stockByStatus(Collections.emptyMap())
            .totalStockTransactions(0L)
            .stockActionBreakdown(Collections.emptyMap())
            .build();
    }

    private SystemStatsDto.OrderStatsDto generateOrderStats() {
        log.debug("Generating order statistics");
        
        try {
            Long totalOrders = orderRepository.count();
            BigDecimal totalRevenue = orderRepository.getTotalRevenue();
            BigDecimal averageOrderValue = orderRepository.getAverageOrderValue();
            
            LocalDate today = LocalDate.now();
            BigDecimal todayRevenue = orderRepository.getRevenueByDateRange(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            BigDecimal weekRevenue = orderRepository.getRevenueByDateRange(
                today.minusWeeks(1).atStartOfDay(), LocalDateTime.now());
            BigDecimal monthRevenue = orderRepository.getRevenueByDateRange(
                today.minusMonths(1).atStartOfDay(), LocalDateTime.now());
            
            // Orders by status
            Map<String, Long> ordersByStatus = orderRepository.countByStatus()
                .stream()
                .collect(Collectors.toMap(
                    result -> result[0].toString(),
                    result -> (Long) result[1]
                ));
            
            // Recent orders
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            PageRequest recentOrdersPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<SystemStatsDto.TopOrderDto> recentOrders = orderRepository.findRecentOrders(oneWeekAgo, recentOrdersPageable)
                .stream()
                .limit(10)
                .map(order -> SystemStatsDto.TopOrderDto.builder()
                    .orderId(order.getId())
                    .userId(order.getUserEmail()) // Using email as user identifier
                    .totalAmount(order.getTotalPrice())
                    .status(order.getStatus().toString())
                    .createdAt(order.getCreatedAt())
                    .build())
                .collect(Collectors.toList());

            return SystemStatsDto.OrderStatsDto.builder()
                .totalOrders(totalOrders)
                .pendingOrders(ordersByStatus.getOrDefault("PENDING", 0L))
                .confirmedOrders(ordersByStatus.getOrDefault("CONFIRMED", 0L))
                .shippedOrders(ordersByStatus.getOrDefault("SHIPPED", 0L))
                .deliveredOrders(ordersByStatus.getOrDefault("DELIVERED", 0L))
                .cancelledOrders(ordersByStatus.getOrDefault("CANCELLED", 0L))
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .averageOrderValue(averageOrderValue != null ? averageOrderValue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weekRevenue(weekRevenue != null ? weekRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .ordersByStatus(ordersByStatus)
                .revenueByPaymentMethod(Collections.emptyMap()) // TODO: Implement payment method breakdown
                .recentOrders(recentOrders)
                .trends(generateOrderTrends())
                .build();
        } catch (Exception e) {
            log.error("Error generating order statistics", e);
            return SystemStatsDto.OrderStatsDto.builder()
                .totalOrders(0L)
                .pendingOrders(0L)
                .confirmedOrders(0L)
                .shippedOrders(0L)
                .deliveredOrders(0L)
                .cancelledOrders(0L)
                .totalRevenue(BigDecimal.ZERO)
                .averageOrderValue(BigDecimal.ZERO)
                .todayRevenue(BigDecimal.ZERO)
                .weekRevenue(BigDecimal.ZERO)
                .monthRevenue(BigDecimal.ZERO)
                .ordersByStatus(Collections.emptyMap())
                .revenueByPaymentMethod(Collections.emptyMap())
                .recentOrders(Collections.emptyList())
                .trends(SystemStatsDto.OrderTrendsDto.builder()
                    .dailyTrends(Collections.emptyList())
                    .monthlyTrends(Collections.emptyList())
                    .ordersByDayOfWeek(Collections.emptyMap())
                    .ordersByHour(Collections.emptyMap())
                    .build())
                .build();
        }
    }

    private SystemStatsDto.CartStatsDto generateCartStats() {
        log.debug("Generating cart statistics");
        
        try {
            Long totalActiveCarts = cartRepository.countActiveCarts();
            Long totalAbandonedCarts = cartRepository.countAbandonedCarts();
            Long totalCartItems = cartItemRepository.count();
            
            BigDecimal totalCartValue = cartRepository.getTotalCartValue();
            BigDecimal averageCartValue = cartRepository.getAverageCartValue();
            
            // Calculate abandonment rate
            BigDecimal abandonmentRate = BigDecimal.ZERO;
            if (totalActiveCarts + totalAbandonedCarts > 0) {
                abandonmentRate = BigDecimal.valueOf(totalAbandonedCarts)
                    .divide(BigDecimal.valueOf(totalActiveCarts + totalAbandonedCarts), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }

            return SystemStatsDto.CartStatsDto.builder()
                .totalActiveCarts(totalActiveCarts != null ? totalActiveCarts : 0L)
                .totalAbandonedCarts(totalAbandonedCarts != null ? totalAbandonedCarts : 0L)
                .totalCartItems(totalCartItems)
                .totalCartValue(totalCartValue != null ? totalCartValue : BigDecimal.ZERO)
                .averageCartValue(averageCartValue != null ? averageCartValue : BigDecimal.ZERO)
                .abandonmentRate(abandonmentRate)
                .mostAddedToCart(Collections.emptyList()) // TODO: Implement when product popularity tracking is available
                .cartsByTimeOfDay(Collections.emptyMap()) // TODO: Implement time-based analysis
                .build();
        } catch (Exception e) {
            log.error("Error generating cart statistics", e);
            return SystemStatsDto.CartStatsDto.builder()
                .totalActiveCarts(0L)
                .totalAbandonedCarts(0L)
                .totalCartItems(0L)
                .totalCartValue(BigDecimal.ZERO)
                .averageCartValue(BigDecimal.ZERO)
                .abandonmentRate(BigDecimal.ZERO)
                .mostAddedToCart(Collections.emptyList())
                .cartsByTimeOfDay(Collections.emptyMap())
                .build();
        }
    }

    private SystemStatsDto.CategoryStatsDto generateCategoryStats() {
        log.debug("Generating category statistics");
        
        try {
            Long totalCategories = categoryRepository.count();
            Long totalDynamicFields = categoryDynamicFieldRepository.count();
            
            // Products by category
            Map<String, Long> productsByCategory = productRepository.countProductsByCategory()
                .stream()
                .collect(Collectors.toMap(
                    result -> (String) result[0],
                    result -> (Long) result[1]
                ));

            return SystemStatsDto.CategoryStatsDto.builder()
                .totalCategories(totalCategories)
                .productsByCategory(productsByCategory)
                .revenueByCategory(Collections.emptyMap()) // TODO: Implement when category-order relationship is established
                .topCategories(Collections.emptyList()) // TODO: Implement ranking logic
                .totalDynamicFields(totalDynamicFields)
                .build();
        } catch (Exception e) {
            log.error("Error generating category statistics", e);
            return SystemStatsDto.CategoryStatsDto.builder()
                .totalCategories(0L)
                .productsByCategory(Collections.emptyMap())
                .revenueByCategory(Collections.emptyMap())
                .topCategories(Collections.emptyList())
                .totalDynamicFields(0L)
                .build();
        }
    }

    private SystemStatsDto.PaymentStatsDto generatePaymentStats() {
        log.debug("Generating payment statistics");
        
        try {
            Long totalPayments = paymentHistoryRepository.count();
            Double successRateDouble = paymentHistoryRepository.getSuccessRate();
            BigDecimal successRate = successRateDouble != null ? BigDecimal.valueOf(successRateDouble) : BigDecimal.ZERO;
            BigDecimal totalSuccessfulAmount = paymentHistoryRepository.getTotalSuccessfulAmount();
            
            // Payments by method
            Map<String, Long> paymentsByMethod = paymentHistoryRepository.countByPaymentMethod()
                .stream()
                .collect(Collectors.toMap(
                    result -> result[0].toString(),
                    result -> (Long) result[1]
                ));
            
            // Payments by status
            Map<String, Long> paymentsByStatus = paymentHistoryRepository.countByStatus()
                .stream()
                .collect(Collectors.toMap(
                    result -> result[0].toString(),
                    result -> (Long) result[1]
                ));

            return SystemStatsDto.PaymentStatsDto.builder()
                .totalPayments(totalPayments)
                .successfulPayments(paymentsByStatus.getOrDefault("SUCCESS", 0L))
                .failedPayments(paymentsByStatus.getOrDefault("FAILED", 0L))
                .pendingPayments(paymentsByStatus.getOrDefault("PENDING", 0L))
                .totalPaymentAmount(totalSuccessfulAmount != null ? totalSuccessfulAmount : BigDecimal.ZERO)
                .successfulPaymentAmount(totalSuccessfulAmount != null ? totalSuccessfulAmount : BigDecimal.ZERO)
                .failedPaymentAmount(BigDecimal.ZERO) // TODO: Calculate failed amount
                .successRate(successRate != null ? successRate : BigDecimal.ZERO)
                .paymentsByMethod(paymentsByMethod)
                .amountByMethod(Collections.emptyMap()) // TODO: Implement amount breakdown by method
                .paymentTrends(Collections.emptyList()) // TODO: Implement trend analysis
                .build();
        } catch (Exception e) {
            log.error("Error generating payment statistics", e);
            return SystemStatsDto.PaymentStatsDto.builder()
                .totalPayments(0L)
                .successfulPayments(0L)
                .failedPayments(0L)
                .pendingPayments(0L)
                .totalPaymentAmount(BigDecimal.ZERO)
                .successfulPaymentAmount(BigDecimal.ZERO)
                .failedPaymentAmount(BigDecimal.ZERO)
                .successRate(BigDecimal.ZERO)
                .paymentsByMethod(Collections.emptyMap())
                .amountByMethod(Collections.emptyMap())
                .paymentTrends(Collections.emptyList())
                .build();
        }
    }

    private SystemStatsDto.UserActivityStatsDto generateUserActivityStats() {
        log.debug("Generating user activity statistics");
        
        try {
            Long totalUsers = profileRepository.count();
            
            // Since detailed user activity tracking is not implemented, 
            // we'll use basic counts from available data
            return SystemStatsDto.UserActivityStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(0L) // TODO: Implement active user tracking
                .newUsersToday(0L) // TODO: Implement new user tracking
                .newUsersThisWeek(0L) // TODO: Implement new user tracking
                .newUsersThisMonth(0L) // TODO: Implement new user tracking
                .userActivityByHour(Collections.emptyMap()) // TODO: Implement hourly analysis
                .topUsers(Collections.emptyList()) // TODO: Implement user ranking
                .build();
        } catch (Exception e) {
            log.error("Error generating user activity statistics", e);
            return SystemStatsDto.UserActivityStatsDto.builder()
                .totalUsers(0L)
                .activeUsers(0L)
                .newUsersToday(0L)
                .newUsersThisWeek(0L)
                .newUsersThisMonth(0L)
                .userActivityByHour(Collections.emptyMap())
                .topUsers(Collections.emptyList())
                .build();
        }
    }

    private SystemStatsDto.PerformanceStatsDto generatePerformanceStats() {
        log.debug("Generating performance statistics");
        
        // This would typically come from application metrics/monitoring
        return SystemStatsDto.PerformanceStatsDto.builder()
            .averageResponseTime(0.0) // TODO: Implement with metrics collection
            .totalApiCalls(0L) // TODO: Implement with metrics collection
            .errorCount(0L) // TODO: Implement with metrics collection
            .errorRate(0.0) // TODO: Implement with metrics collection
            .endpointUsage(Collections.emptyMap()) // TODO: Implement with metrics collection
            .build();
    }

    private SystemStatsDto.OrderTrendsDto generateOrderTrends() {
        log.debug("Generating order trends");
        
        try {
            // Daily trends for last 30 days
            List<SystemStatsDto.DailyTrendDto> dailyTrends = orderRepository.getDailyOrderTrends()
                .stream()
                .map(result -> SystemStatsDto.DailyTrendDto.builder()
                    .date(result[0].toString())
                    .orderCount((Long) result[1])
                    .revenue((BigDecimal) result[2])
                    .build())
                .collect(Collectors.toList());
            
            // Monthly trends for last 12 months
            List<SystemStatsDto.MonthlyTrendDto> monthlyTrends = orderRepository.getMonthlyOrderTrends()
                .stream()
                .map(result -> SystemStatsDto.MonthlyTrendDto.builder()
                    .month(result[0].toString())
                    .orderCount((Long) result[1])
                    .revenue((BigDecimal) result[2])
                    .build())
                .collect(Collectors.toList());

            return SystemStatsDto.OrderTrendsDto.builder()
                .dailyTrends(dailyTrends)
                .monthlyTrends(monthlyTrends)
                .ordersByDayOfWeek(Collections.emptyMap()) // TODO: Implement day of week analysis
                .ordersByHour(Collections.emptyMap()) // TODO: Implement hourly analysis
                .build();
        } catch (Exception e) {
            log.error("Error generating order trends", e);
            return SystemStatsDto.OrderTrendsDto.builder()
                .dailyTrends(Collections.emptyList())
                .monthlyTrends(Collections.emptyList())
                .ordersByDayOfWeek(Collections.emptyMap())
                .ordersByHour(Collections.emptyMap())
                .build();
        }
    }

    private SystemStatsDto getErrorStats(Exception e) {
        log.error("Returning error stats due to exception", e);
        
        return SystemStatsDto.builder()
            .generatedAt(LocalDateTime.now())
            .systemStatus("ERROR: " + e.getMessage())
            .productStats(SystemStatsDto.ProductStatsDto.builder().build())
            .stockStats(SystemStatsDto.StockStatsDto.builder().build())
            .orderStats(SystemStatsDto.OrderStatsDto.builder().build())
            .cartStats(SystemStatsDto.CartStatsDto.builder().build())
            .categoryStats(SystemStatsDto.CategoryStatsDto.builder().build())
            .paymentStats(SystemStatsDto.PaymentStatsDto.builder().build())
            .userActivityStats(SystemStatsDto.UserActivityStatsDto.builder().build())
            .performanceStats(SystemStatsDto.PerformanceStatsDto.builder().build())
            .build();
    }
}

package com.hungng3011.vdtecomberefresh.order.repositories;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    
    // Basic secure filtering methods using parameterized queries
    
    // Cursor-based pagination methods
    
    /**
     * Find orders with cursor-based pagination (next page)
     * Orders are sorted by createdAt DESC and id ASC for consistent ordering
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:cursor IS NULL OR o.id > :cursor) " +
           "ORDER BY o.id ASC")
    List<Order> findWithCursorAfter(@Param("cursor") String cursor, Pageable pageable);
    
    /**
     * Find orders with cursor-based pagination (previous page)
     * Orders are sorted by createdAt DESC and id DESC for consistent ordering
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:cursor IS NULL OR o.id < :cursor) " +
           "ORDER BY o.id DESC")
    List<Order> findWithCursorBefore(@Param("cursor") String cursor, Pageable pageable);
    
    /**
     * Count total orders for pagination metadata
     */
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();
    
    /**
     * Find orders by user email with secure parameterized query
     */
    Page<Order> findByUserEmail(String userEmail, Pageable pageable);
    
    /**
     * Find orders by status with secure parameterized query
     */
    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);
    
    /**
     * Find orders by payment status with secure parameterized query
     */
    Page<Order> findByPaymentStatusIn(List<PaymentStatus> paymentStatuses, Pageable pageable);
    
    /**
     * Comprehensive order filtering with multiple criteria using secure parameterized queries
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:userEmail IS NULL OR o.userEmail = :userEmail) AND " +
           "(:orderStatuses IS NULL OR o.status IN :orderStatuses) AND " +
           "(:paymentStatuses IS NULL OR o.paymentStatus IN :paymentStatuses) AND " +
           "(:paymentMethods IS NULL OR o.paymentMethod IN :paymentMethods) AND " +
           "(:phone IS NULL OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
           "(:address IS NULL OR LOWER(o.address) LIKE LOWER(CONCAT('%', :address, '%'))) AND " +
           "(:minTotalPrice IS NULL OR o.totalPrice >= :minTotalPrice) AND " +
           "(:maxTotalPrice IS NULL OR o.totalPrice <= :maxTotalPrice) AND " +
           "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
           "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) AND " +
           "(:updatedAfter IS NULL OR o.updatedAt >= :updatedAfter) AND " +
           "(:updatedBefore IS NULL OR o.updatedAt <= :updatedBefore) AND " +
           "(:paymentId IS NULL OR o.paymentId = :paymentId)")
    Page<Order> findByMultipleCriteria(
            @Param("userEmail") String userEmail,
            @Param("orderStatuses") List<OrderStatus> orderStatuses,
            @Param("paymentStatuses") List<PaymentStatus> paymentStatuses,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            @Param("phone") String phone,
            @Param("address") String address,
            @Param("minTotalPrice") BigDecimal minTotalPrice,
            @Param("maxTotalPrice") BigDecimal maxTotalPrice,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("updatedAfter") LocalDateTime updatedAfter,
            @Param("updatedBefore") LocalDateTime updatedBefore,
            @Param("paymentId") String paymentId,
            Pageable pageable);
    
    /**
     * Find orders containing specific products with secure parameterized queries
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.items oi " +
           "LEFT JOIN oi.product p " +
           "WHERE " +
           "(:userEmail IS NULL OR o.userEmail = :userEmail) AND " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) AND " +
           "(:orderStatuses IS NULL OR o.status IN :orderStatuses) AND " +
           "(:paymentStatuses IS NULL OR o.paymentStatus IN :paymentStatuses)")
    Page<Order> findByProductCriteria(
            @Param("userEmail") String userEmail,
            @Param("productId") Long productId,
            @Param("productName") String productName,
            @Param("orderStatuses") List<OrderStatus> orderStatuses,
            @Param("paymentStatuses") List<PaymentStatus> paymentStatuses,
            Pageable pageable);
    
    /**
     * Find orders by date range with secure parameterized queries
     */
    @Query("SELECT o FROM Order o WHERE " +
           "o.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * Payment-focused order queries for payment filtering
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:userEmail IS NULL OR o.userEmail = :userEmail) AND " +
           "(:orderId IS NULL OR o.id = :orderId) AND " +
           "(:paymentId IS NULL OR o.paymentId = :paymentId) AND " +
           "(:paymentStatuses IS NULL OR o.paymentStatus IN :paymentStatuses) AND " +
           "(:paymentMethods IS NULL OR o.paymentMethod IN :paymentMethods) AND " +
           "(:minAmount IS NULL OR o.totalPrice >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalPrice <= :maxAmount) AND " +
           "(:paymentDateAfter IS NULL OR o.updatedAt >= :paymentDateAfter) AND " +
           "(:paymentDateBefore IS NULL OR o.updatedAt <= :paymentDateBefore) AND " +
           "(:orderCreatedAfter IS NULL OR o.createdAt >= :orderCreatedAfter) AND " +
           "(:orderCreatedBefore IS NULL OR o.createdAt <= :orderCreatedBefore)")
    Page<Order> findByPaymentCriteria(
            @Param("userEmail") String userEmail,
            @Param("orderId") String orderId,
            @Param("paymentId") String paymentId,
            @Param("paymentStatuses") List<PaymentStatus> paymentStatuses,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("paymentDateAfter") LocalDateTime paymentDateAfter,
            @Param("paymentDateBefore") LocalDateTime paymentDateBefore,
            @Param("orderCreatedAfter") LocalDateTime orderCreatedAfter,
            @Param("orderCreatedBefore") LocalDateTime orderCreatedBefore,
            Pageable pageable);
    
    /**
     * Get payment statistics with secure aggregation queries
     */
    @Query("SELECT " +
           "COUNT(o) as totalOrders, " +
           "COUNT(CASE WHEN o.paymentStatus = 'SUCCESSFUL' THEN 1 END) as successfulPayments, " +
           "COUNT(CASE WHEN o.paymentStatus = 'FAILED' THEN 1 END) as failedPayments, " +
           "COUNT(CASE WHEN o.paymentStatus = 'REFUNDED' THEN 1 END) as refundedPayments, " +
           "SUM(CASE WHEN o.paymentStatus = 'SUCCESSFUL' THEN o.totalPrice ELSE 0 END) as totalRevenue " +
           "FROM Order o WHERE " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Object[] getPaymentStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Statistical queries for system stats
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.paymentStatus = 'SUCCESSFUL'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();
    
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.paymentStatus = 'SUCCESSFUL' AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal getRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.paymentStatus = 'SUCCESSFUL'")
    BigDecimal getAverageOrderValue();
    
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT DATE(o.createdAt) as date, COUNT(o) as count, COALESCE(SUM(CASE WHEN o.paymentStatus = 'SUCCESSFUL' THEN o.totalPrice ELSE 0 END), 0) as revenue " +
           "FROM Order o WHERE o.createdAt >= :startDate GROUP BY DATE(o.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyOrderTrends(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT DATE_FORMAT(o.createdAt, '%Y-%m') as month, COUNT(o) as count, COALESCE(SUM(CASE WHEN o.paymentStatus = 'SUCCESSFUL' THEN o.totalPrice ELSE 0 END), 0) as revenue " +
           "FROM Order o WHERE o.createdAt >= :startDate GROUP BY DATE_FORMAT(o.createdAt, '%Y-%m') ORDER BY month DESC")
    List<Object[]> getMonthlyOrderTrends(@Param("startDate") LocalDateTime startDate);
    
    // Default methods for convenience
    default List<Object[]> getDailyOrderTrends() {
        return getDailyOrderTrends(LocalDateTime.now().minusDays(30));
    }
    
    default List<Object[]> getMonthlyOrderTrends() {
        return getMonthlyOrderTrends(LocalDateTime.now().minusMonths(12));
    }
}

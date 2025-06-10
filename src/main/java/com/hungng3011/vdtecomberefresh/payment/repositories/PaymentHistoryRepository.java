package com.hungng3011.vdtecomberefresh.payment.repositories;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import com.hungng3011.vdtecomberefresh.payment.entities.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    
    // Cursor-based pagination methods
    
    /**
     * Find payment history with cursor-based pagination (next page)
     * Payments are sorted by paymentDate DESC and id ASC for consistent ordering
     */
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "(:cursor IS NULL OR p.id > :cursor) " +
           "ORDER BY p.id ASC")
    List<PaymentHistory> findWithCursorAfter(@Param("cursor") Long cursor, Pageable pageable);
    
    /**
     * Find payment history with cursor-based pagination (previous page)
     * Payments are sorted by paymentDate DESC and id DESC for consistent ordering
     */
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "(:cursor IS NULL OR p.id < :cursor) " +
           "ORDER BY p.id DESC")
    List<PaymentHistory> findWithCursorBefore(@Param("cursor") Long cursor, Pageable pageable);
    
    /**
     * Count total payment history records for pagination metadata
     */
    @Query("SELECT COUNT(p) FROM PaymentHistory p")
    long countAllPaymentHistory();
    
    // User-specific cursor-based pagination
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.userId = :userId AND " +
           "(:cursor IS NULL OR p.id > :cursor) " +
           "ORDER BY p.id ASC")
    List<PaymentHistory> findByUserIdWithCursorAfter(@Param("userId") String userId, 
                                                     @Param("cursor") Long cursor, 
                                                     Pageable pageable);
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.userId = :userId AND " +
           "(:cursor IS NULL OR p.id < :cursor) " +
           "ORDER BY p.id DESC")
    List<PaymentHistory> findByUserIdWithCursorBefore(@Param("userId") String userId, 
                                                      @Param("cursor") Long cursor, 
                                                      Pageable pageable);
    
    long countByUserId(String userId);
    
    // Status-specific cursor-based pagination
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.status = :status AND " +
           "(:cursor IS NULL OR p.id > :cursor) " +
           "ORDER BY p.id ASC")
    List<PaymentHistory> findByStatusWithCursorAfter(@Param("status") PaymentStatus status,
                                                     @Param("cursor") Long cursor, 
                                                     Pageable pageable);
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.status = :status AND " +
           "(:cursor IS NULL OR p.id < :cursor) " +
           "ORDER BY p.id DESC")
    List<PaymentHistory> findByStatusWithCursorBefore(@Param("status") PaymentStatus status,
                                                      @Param("cursor") Long cursor, 
                                                      Pageable pageable);
    
    long countByStatus(PaymentStatus status);
    
    // Payment method-specific cursor-based pagination
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.paymentMethod = :paymentMethod AND " +
           "(:cursor IS NULL OR p.id > :cursor) " +
           "ORDER BY p.id ASC")
    List<PaymentHistory> findByPaymentMethodWithCursorAfter(@Param("paymentMethod") PaymentMethod paymentMethod,
                                                            @Param("cursor") Long cursor, 
                                                            Pageable pageable);
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.paymentMethod = :paymentMethod AND " +
           "(:cursor IS NULL OR p.id < :cursor) " +
           "ORDER BY p.id DESC")
    List<PaymentHistory> findByPaymentMethodWithCursorBefore(@Param("paymentMethod") PaymentMethod paymentMethod,
                                                             @Param("cursor") Long cursor, 
                                                             Pageable pageable);
    
    long countByPaymentMethod(PaymentMethod paymentMethod);

    // Basic filtering methods
    
    /**
     * Find payment history by user ID with secure parameterized query
     */
    Page<PaymentHistory> findByUserId(String userId, Pageable pageable);
    
    /**
     * Find payment history by order ID with secure parameterized query
     */
    Page<PaymentHistory> findByOrderId(String orderId, Pageable pageable);
    
    /**
     * Find payment history by status with secure parameterized query
     */
    Page<PaymentHistory> findByStatusIn(List<PaymentStatus> statuses, Pageable pageable);
    
    /**
     * Find payment history by payment method with secure parameterized query
     */
    Page<PaymentHistory> findByPaymentMethodIn(List<PaymentMethod> paymentMethods, Pageable pageable);
    
    /**
     * Comprehensive payment history filtering with multiple criteria
     */
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "(:userId IS NULL OR p.userId = :userId) AND " +
           "(:orderId IS NULL OR p.orderId = :orderId) AND " +
           "(:gatewayTransactionId IS NULL OR p.gatewayTransactionId = :gatewayTransactionId) AND " +
           "(:statuses IS NULL OR p.status IN :statuses) AND " +
           "(:paymentMethods IS NULL OR p.paymentMethod IN :paymentMethods) AND " +
           "(:minAmount IS NULL OR p.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR p.amount <= :maxAmount) AND " +
           "(:paymentDateAfter IS NULL OR p.paymentDate >= :paymentDateAfter) AND " +
           "(:paymentDateBefore IS NULL OR p.paymentDate <= :paymentDateBefore) AND " +
           "(:createdAfter IS NULL OR p.createdAt >= :createdAfter) AND " +
           "(:createdBefore IS NULL OR p.createdAt <= :createdBefore)")
    Page<PaymentHistory> findByMultipleCriteria(
            @Param("userId") String userId,
            @Param("orderId") String orderId,
            @Param("gatewayTransactionId") String gatewayTransactionId,
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("paymentDateAfter") LocalDateTime paymentDateAfter,
            @Param("paymentDateBefore") LocalDateTime paymentDateBefore,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore,
            Pageable pageable);
    
    /**
     * Find payment history by date range with secure parameterized queries
     */
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "p.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.paymentDate DESC")
    Page<PaymentHistory> findByPaymentDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * Get payment statistics with secure aggregation queries
     */
    @Query("SELECT " +
           "COUNT(p) as totalPayments, " +
           "COUNT(CASE WHEN p.status = 'SUCCESSFUL' THEN 1 END) as successfulPayments, " +
           "COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) as failedPayments, " +
           "COUNT(CASE WHEN p.status = 'REFUNDED' THEN 1 END) as refundedPayments, " +
           "SUM(CASE WHEN p.status = 'SUCCESSFUL' THEN p.amount ELSE 0 END) as totalRevenue " +
           "FROM PaymentHistory p WHERE " +
           "(:startDate IS NULL OR p.paymentDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.paymentDate <= :endDate)")
    Object[] getPaymentStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Additional statistical queries for system stats
    @Query("SELECT p.paymentMethod, COUNT(p) FROM PaymentHistory p GROUP BY p.paymentMethod")
    List<Object[]> countByPaymentMethod();
    
    @Query("SELECT p.status, COUNT(p) FROM PaymentHistory p GROUP BY p.status")
    List<Object[]> countByStatus();
    
    @Query("SELECT COUNT(p) * 100.0 / (SELECT COUNT(total) FROM PaymentHistory total) FROM PaymentHistory p WHERE p.status = 'SUCCESSFUL'")
    Double getSuccessRate();
    
    @Query("SELECT SUM(p.amount) FROM PaymentHistory p WHERE p.status = 'SUCCESSFUL'")
    BigDecimal getTotalSuccessfulAmount();
    
    @Query("SELECT AVG(p.amount) FROM PaymentHistory p WHERE p.status = 'SUCCESSFUL'")
    BigDecimal getAverageSuccessfulAmount();
}

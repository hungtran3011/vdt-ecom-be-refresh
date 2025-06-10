package com.hungng3011.vdtecomberefresh.cart.repositories;

import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * Find carts with cursor-based pagination (forward)
     * @param cursor The cursor (cart ID) to start from, or null for first page
     * @param pageable Pageable information for limiting results
     * @return List of carts after the cursor
     */
    @Query("SELECT c FROM Cart c WHERE (:cursor IS NULL OR c.id > :cursor) ORDER BY c.id ASC")
    List<Cart> findWithCursorAfter(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * Find carts with cursor-based pagination (backward)
     * @param cursor The cursor (cart ID) to start from
     * @param pageable Pageable information for limiting results
     * @return List of carts before the cursor (in DESC order)
     */
    @Query("SELECT c FROM Cart c WHERE c.id < :cursor ORDER BY c.id DESC")
    List<Cart> findWithCursorBefore(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * Count all carts
     * @return Total number of carts
     */
    @Query("SELECT COUNT(c) FROM Cart c")
    long countAllCarts();

    /**
     * Find carts by user ID with cursor-based pagination (forward)
     * @param userId User ID to filter by
     * @param cursor The cursor (cart ID) to start from, or null for first page
     * @param pageable Pageable information for limiting results
     * @return List of carts after the cursor for the specific user
     */
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND (:cursor IS NULL OR c.id > :cursor) ORDER BY c.id ASC")
    List<Cart> findByUserIdWithCursorAfter(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Find carts by user ID with cursor-based pagination (backward)
     * @param userId User ID to filter by
     * @param cursor The cursor (cart ID) to start from
     * @param pageable Pageable information for limiting results
     * @return List of carts before the cursor for the specific user (in DESC order)
     */
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.id < :cursor ORDER BY c.id DESC")
    List<Cart> findByUserIdWithCursorBefore(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Count carts by user ID
     * @param userId User ID to count carts for
     * @return Total number of carts for the user
     */
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Statistical queries for system stats
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.lastUpdated >= :activeThreshold")
    long countActiveCarts(@Param("activeThreshold") LocalDateTime activeThreshold);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.lastUpdated < :abandonedThreshold AND SIZE(c.items) > 0")
    long countAbandonedCarts(@Param("abandonedThreshold") LocalDateTime abandonedThreshold);
    
    @Query("SELECT COALESCE(SUM(ci.quantity * ci.unitPrice), 0) FROM Cart c JOIN c.items ci")
    BigDecimal getTotalCartValue();
    
    @Query("SELECT COALESCE(AVG(subquery.cartValue), 0) FROM " +
           "(SELECT SUM(ci.quantity * ci.unitPrice) as cartValue FROM Cart c JOIN c.items ci GROUP BY c.id) subquery")
    BigDecimal getAverageCartValue();
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE SIZE(c.items) > 0")
    long countNonEmptyCarts();
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE SIZE(c.items) = 0")
    long countEmptyCarts();
    
    default long countActiveCarts() {
        return countActiveCarts(LocalDateTime.now().minusDays(7)); // Active in last 7 days
    }
    
    default long countAbandonedCarts() {
        return countAbandonedCarts(LocalDateTime.now().minusDays(7)); // Abandoned after 7 days
    }
}

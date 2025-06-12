package com.hungng3011.vdtecomberefresh.cart.repositories;

import com.hungng3011.vdtecomberefresh.cart.entities.CartItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * Find cart items for a specific cart with cursor-based pagination (forward)
     * @param cartId Cart ID to filter by
     * @param cursor The cursor (cart item ID) to start from, or null for first page
     * @param pageable Pageable information for limiting results
     * @return List of cart items after the cursor for the specific cart
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND (:cursor IS NULL OR ci.id > :cursor) ORDER BY ci.id ASC")
    List<CartItem> findByCartIdWithCursorAfter(@Param("cartId") Long cartId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Find cart items for a specific cart with cursor-based pagination (backward)
     * @param cartId Cart ID to filter by
     * @param cursor The cursor (cart item ID) to start from
     * @param pageable Pageable information for limiting results
     * @return List of cart items before the cursor for the specific cart (in DESC order)
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.id < :cursor ORDER BY ci.id DESC")
    List<CartItem> findByCartIdWithCursorBefore(@Param("cartId") Long cartId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Count cart items for a specific cart
     * @param cartId Cart ID to count items for
     * @return Total number of items in the cart
     */
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    long countByCartId(@Param("cartId") Long cartId);

    /**
     * Delete all cart items for a specific cart
     * @param cartId Cart ID to delete items from
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    /**
     * Find cart items by user ID (through cart) with cursor-based pagination (forward)
     * @param userId User ID to filter by
     * @param cursor The cursor (cart item ID) to start from, or null for first page
     * @param pageable Pageable information for limiting results
     * @return List of cart items after the cursor for the specific user
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.userId = :userId AND (:cursor IS NULL OR ci.id > :cursor) ORDER BY ci.id ASC")
    List<CartItem> findByUserIdWithCursorAfter(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Find cart items by user ID (through cart) with cursor-based pagination (backward)
     * @param userId User ID to filter by
     * @param cursor The cursor (cart item ID) to start from
     * @param pageable Pageable information for limiting results
     * @return List of cart items before the cursor for the specific user (in DESC order)
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.userId = :userId AND ci.id < :cursor ORDER BY ci.id DESC")
    List<CartItem> findByUserIdWithCursorBefore(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * Count cart items by user ID (through cart)
     * @param userId User ID to count items for
     * @return Total number of items in all user's carts
     */
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}

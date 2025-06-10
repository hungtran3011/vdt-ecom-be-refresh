package com.hungng3011.vdtecomberefresh.stock.repositories;

import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByProductId(Long id);
    
    /**
     * Find stock by product ID and specific variation IDs
     * This supports your requirement for variation-specific stock lookup
     */
    @Query("SELECT s FROM Stock s JOIN s.variations v WHERE s.product.id = :productId AND v.id IN :variationIds " +
           "GROUP BY s HAVING COUNT(DISTINCT v.id) = :variationCount")
    List<Stock> findByProductIdAndVariationIds(@Param("productId") Long productId, 
                                              @Param("variationIds") List<Long> variationIds,
                                              @Param("variationCount") long variationCount);
    
    /**
     * Find stock by product ID and variation combinations (exact match)
     * Returns stock that has exactly the specified variations
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND " +
           "SIZE(s.variations) = :variationCount AND " +
           "NOT EXISTS (SELECT 1 FROM Stock s2 JOIN s2.variations v WHERE s2.id = s.id AND v.id NOT IN :variationIds)")
    List<Stock> findByProductIdAndExactVariations(@Param("productId") Long productId, 
                                                 @Param("variationIds") List<Long> variationIds,
                                                 @Param("variationCount") int variationCount);
    
    /**
     * Find available stock (IN_STOCK or LOW_STOCK) by product and variations
     */
    @Query("SELECT s FROM Stock s JOIN s.variations v WHERE s.product.id = :productId AND v.id IN :variationIds " +
           "AND s.status IN ('IN_STOCK', 'LOW_STOCK') " +
           "GROUP BY s HAVING COUNT(DISTINCT v.id) = :variationCount")
    List<Stock> findAvailableByProductIdAndVariationIds(@Param("productId") Long productId, 
                                                       @Param("variationIds") List<Long> variationIds,
                                                       @Param("variationCount") long variationCount);
    
    /**
     * Find stock by SKU - useful for cart/order operations
     */
    Optional<Stock> findBySku(String sku);
    
    /**
     * Check if specific variation combination exists for a product
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Stock s JOIN s.variations v " +
           "WHERE s.product.id = :productId AND v.id IN :variationIds " +
           "GROUP BY s HAVING COUNT(DISTINCT v.id) = :variationCount")
    boolean existsByProductIdAndVariationIds(@Param("productId") Long productId, 
                                           @Param("variationIds") List<Long> variationIds,
                                           @Param("variationCount") long variationCount);
    
    /**
     * Get all available variation combinations for a product
     * Returns stocks that have quantity > 0
     */
    @Query("SELECT DISTINCT s FROM Stock s WHERE s.product.id = :productId AND s.quantity > 0")
    List<Stock> findAvailableVariationsByProductId(@Param("productId") Long productId);
    
    // Statistical queries for system stats
    @Query("SELECT SUM(s.quantity * s.product.basePrice) FROM Stock s WHERE s.quantity > 0")
    BigDecimal getTotalStockValue();
    
    @Query("SELECT s FROM Stock s WHERE s.quantity <= s.lowStockThreshold AND s.lowStockThreshold > 0")
    List<Stock> findLowStockItems();
    
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.quantity > 0")
    long countInStockItems();
    
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.quantity = 0")
    long countOutOfStockItems();
    
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.quantity <= s.lowStockThreshold AND s.lowStockThreshold > 0")
    long countLowStockItems();
    
    @Query("SELECT s.status, COUNT(s) FROM Stock s GROUP BY s.status")
    List<Object[]> countByStatus();
    
    @Query("SELECT SUM(s.quantity) FROM Stock s")
    Long getTotalQuantityInStock();
    
    @Query("SELECT COUNT(DISTINCT s.product.id) FROM Stock s WHERE s.quantity > 0")
    long countDistinctProductsInStock();
}

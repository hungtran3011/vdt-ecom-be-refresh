package com.hungng3011.vdtecomberefresh.product.repositories;

import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    long countByCategory(Category category);
    
    // Cursor-based pagination for products by category
    Page<Product> findByCategory(Category category, Pageable pageable);
    
    // Cursor-based pagination with cursor (ID-based)
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id > :cursor ORDER BY p.id ASC")
    List<Product> findByCategoryWithCursorAfter(@Param("category") Category category, 
                                               @Param("cursor") Long cursor, 
                                               Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id < :cursor ORDER BY p.id DESC")
    List<Product> findByCategoryWithCursorBefore(@Param("category") Category category, 
                                                @Param("cursor") Long cursor, 
                                                Pageable pageable);
    
    // General cursor-based pagination for all products
    @Query("SELECT p FROM Product p WHERE (:cursor IS NULL OR p.id > :cursor) ORDER BY p.id ASC")
    List<Product> findAllWithCursorAfter(@Param("cursor") Long cursor, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Product> findAllWithCursorBefore(@Param("cursor") Long cursor, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p")
    long countAllProducts();
    
    // Secure filtering methods using parameterized queries to prevent SQL injection
    
    /**
     * Filter products by name with case-insensitive partial matching
     * Uses parameterized query to prevent SQL injection
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);
    
    /**
     * Filter products by multiple criteria with secure parameterized queries
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice)")
    Page<Product> findByMultipleCriteria(
            @Param("name") String name,
            @Param("description") String description,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
    
    /**
     * Filter products by dynamic field values with secure parameterized queries
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN p.dynamicValues dv " +
           "JOIN dv.field df " +
           "WHERE " +
           "(:fieldName IS NULL OR LOWER(df.fieldName) = LOWER(:fieldName)) AND " +
           "(:fieldValue IS NULL OR " +
           "  CASE :matchType " +
           "    WHEN 'EQUALS' THEN LOWER(dv.value) = LOWER(:fieldValue) " +
           "    WHEN 'CONTAINS' THEN LOWER(dv.value) LIKE LOWER(CONCAT('%', :fieldValue, '%')) " +
           "    WHEN 'STARTS_WITH' THEN LOWER(dv.value) LIKE LOWER(CONCAT(:fieldValue, '%')) " +
           "    WHEN 'ENDS_WITH' THEN LOWER(dv.value) LIKE LOWER(CONCAT('%', :fieldValue)) " +
           "    ELSE TRUE " +
           "  END)")
    Page<Product> findByDynamicField(
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue,
            @Param("matchType") String matchType,
            Pageable pageable);
    
    /**
     * Combined filter with dynamic fields - most comprehensive search
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.dynamicValues dv " +
           "LEFT JOIN dv.field df " +
           "WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice) AND " +
           "(:fieldName IS NULL OR " +
           "  EXISTS (SELECT 1 FROM ProductDynamicValue pdv " +
           "          JOIN pdv.field pf " +
           "          WHERE pdv.product = p AND " +
           "                LOWER(pf.fieldName) = LOWER(:fieldName) AND " +
           "                CASE :matchType " +
           "                  WHEN 'EQUALS' THEN LOWER(pdv.value) = LOWER(:fieldValue) " +
           "                  WHEN 'CONTAINS' THEN LOWER(pdv.value) LIKE LOWER(CONCAT('%', :fieldValue, '%')) " +
           "                  WHEN 'STARTS_WITH' THEN LOWER(pdv.value) LIKE LOWER(CONCAT(:fieldValue, '%')) " +
           "                  WHEN 'ENDS_WITH' THEN LOWER(pdv.value) LIKE LOWER(CONCAT('%', :fieldValue)) " +
           "                  ELSE TRUE " +
           "                END))")
    Page<Product> findByComprehensiveCriteria(
            @Param("name") String name,
            @Param("description") String description,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue,
            @Param("matchType") String matchType,
            Pageable pageable);
    
    /**
     * Count products by multiple criteria - matching findByMultipleCriteria
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice)")
    long countByMultipleCriteria(
            @Param("name") String name,
            @Param("description") String description,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * Count products by comprehensive criteria - matching findByComprehensiveCriteria
     */
    @Query("SELECT COUNT(DISTINCT p) FROM Product p " +
           "LEFT JOIN p.dynamicValues dv " +
           "LEFT JOIN dv.field df " +
           "WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice) AND " +
           "(:fieldName IS NULL OR " +
           "  EXISTS (SELECT 1 FROM ProductDynamicValue pdv " +
           "          JOIN pdv.field pf " +
           "          WHERE pdv.product = p AND " +
           "                LOWER(pf.fieldName) = LOWER(:fieldName) AND " +
           "                CASE :matchType " +
           "                  WHEN 'EQUALS' THEN LOWER(pdv.value) = LOWER(:fieldValue) " +
           "                  WHEN 'CONTAINS' THEN LOWER(pdv.value) LIKE LOWER(CONCAT('%', :fieldValue, '%')) " +
           "                  WHEN 'STARTS_WITH' THEN LOWER(pdv.value) LIKE LOWER(CONCAT(:fieldValue, '%')) " +
           "                  WHEN 'ENDS_WITH' THEN LOWER(pdv.value) LIKE LOWER(CONCAT('%', :fieldValue)) " +
           "                  ELSE TRUE " +
           "                END))")
    long countByComprehensiveCriteria(
            @Param("name") String name,
            @Param("description") String description,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue,
            @Param("matchType") String matchType);
    
    // Statistical queries for system stats
    @Query("SELECT COUNT(p) FROM Product p WHERE EXISTS (SELECT 1 FROM Stock s WHERE s.product = p AND SIZE(s.variations) > 0)")
    long countProductsWithVariations();
    
    @Query("SELECT AVG(p.basePrice) FROM Product p")
    BigDecimal getAverageProductPrice();
    
    @Query("SELECT c.name, COUNT(p) FROM Product p JOIN p.category c GROUP BY c.name")
    List<Object[]> countProductsByCategory();
    
    @Query("SELECT MIN(p.basePrice) FROM Product p")
    BigDecimal getMinProductPrice();
    
    @Query("SELECT MAX(p.basePrice) FROM Product p")
    BigDecimal getMaxProductPrice();
    
    // Methods needed for search indexing
    
    /**
     * Find products updated after a specific timestamp for incremental indexing
     */
    @Query("SELECT p FROM Product p WHERE p.updatedAt > :timestamp")
    List<Product> findByUpdatedAtAfter(@Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Find products by category ID for bulk operations
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
}

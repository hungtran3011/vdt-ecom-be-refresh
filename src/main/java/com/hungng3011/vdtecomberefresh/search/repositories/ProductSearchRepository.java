package com.hungng3011.vdtecomberefresh.search.repositories;

import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch repository for product search operations
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {
    
    /**
     * Full-text search across multiple fields with boosting
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "description^2", "brand^2", "tags^1.5", "keywords^1.5", "autocompleteText^2"],
                  "type": "best_fields",
                  "fuzziness": "AUTO"
                }
              },
              {
                "match_phrase_prefix": {
                  "autocompleteText": {
                    "query": "?0",
                    "boost": 2
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<ProductSearchDocument> findByMultiFieldSearch(String query, Pageable pageable);
    
    /**
     * Search with category filter
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "description^2", "brand^2", "tags^1.5"],
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "categoryId": "?1"
                }
              }
            ]
          }
        }
        """)
    Page<ProductSearchDocument> findByQueryAndCategory(String query, Long categoryId, Pageable pageable);
    
    /**
     * Search with price range filter
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "description^2", "brand^2", "tags^1.5"],
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "range": {
                  "basePrice": {
                    "gte": "?1",
                    "lte": "?2"
                  }
                }
              }
            ]
          }
        }
        """)
    Page<ProductSearchDocument> findByQueryAndPriceRange(String query, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    /**
     * Find products by category
     */
    Page<ProductSearchDocument> findByCategoryId(Long categoryId, Pageable pageable);
    
    /**
     * Find products by brand
     */
    Page<ProductSearchDocument> findByBrand(String brand, Pageable pageable);
    
    /**
     * Find products in stock
     */
    Page<ProductSearchDocument> findByInStock(Boolean inStock, Pageable pageable);
    
    /**
     * Find featured products
     */
    Page<ProductSearchDocument> findByIsFeatured(Boolean isFeatured, Pageable pageable);
    
    /**
     * Find products by tags
     */
    Page<ProductSearchDocument> findByTagsIn(List<String> tags, Pageable pageable);
    
    /**
     * Find products by price range
     */
    Page<ProductSearchDocument> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    /**
     * Find products by minimum rating
     */
    Page<ProductSearchDocument> findByAverageRatingGreaterThanEqual(Float minRating, Pageable pageable);
    
    /**
     * Find products by status
     */
    Page<ProductSearchDocument> findByIsActiveAndIsVisible(Boolean isActive, Boolean isVisible, Pageable pageable);
    
    /**
     * Additional methods for simplified search service
     */
    Page<ProductSearchDocument> findByNameContaining(String name, Pageable pageable);
    
    Page<ProductSearchDocument> findBySearchableTextContaining(String text, Pageable pageable);
    
    Page<ProductSearchDocument> findByAutocompleteTextContaining(String text, Pageable pageable);
    
    @Query("{\"bool\": {\"should\": [{\"match\": {\"name\": {\"query\": \"?0\", \"boost\": 3}}}, {\"match\": {\"description\": {\"query\": \"?0\", \"boost\": 2}}}, {\"match\": {\"brand\": {\"query\": \"?0\", \"boost\": 2}}}]}}")
    Page<ProductSearchDocument> findByMultipleFields(String query, Pageable pageable);
    
    /**
     * Simplified facet queries (would need proper implementation)
     */
    default List<Object[]> findCategoryFacets() {
        return List.of(new Object[]{"Electronics", 10L}, new Object[]{"Clothing", 5L});
    }
    
    default List<Object[]> findBrandFacets() {
        return List.of(new Object[]{"Apple", 8L}, new Object[]{"Samsung", 6L});
    }
}

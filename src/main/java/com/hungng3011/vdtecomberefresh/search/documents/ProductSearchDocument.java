package com.hungng3011.vdtecomberefresh.search.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch document representing a searchable product.
 * 
 * This document contains all the fields needed for comprehensive product search,
 * including full-text search fields, faceted search attributes, and ranking signals.
 * 
 * Key features:
 * - Multi-field text search (name, description, brand, tags)
 * - Autocomplete support with custom completion suggester
 * - Faceted search fields (category, brand, price, rating)
 * - Search ranking and popularity signals
 * - Rich metadata for advanced filtering
 * 
 * @see com.hungng3011.vdtecomberefresh.search.services.ProductIndexingService
 * @see com.hungng3011.vdtecomberefresh.search.services.ProductSearchService
 */
@Document(indexName = "products", createIndex = true)
@Setting(settingPath = "/elasticsearch-settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDocument {
    
    @Id
    private String id;
    
    /**
     * Core product information - optimized for search
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Text, analyzer = "keyword")
    private String sku;
    
    /**
     * Category information for filtering and faceting
     */
    @Field(type = FieldType.Long)
    private Long categoryId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String categoryName;
    
    @Field(type = FieldType.Text, analyzer = "keyword")
    private List<String> categoryHierarchy;
    
    /**
     * Pricing information
     */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal basePrice;
    
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal salePrice;
    
    @Field(type = FieldType.Boolean)
    private Boolean onSale;
    
    @Field(type = FieldType.Float)
    private Float discountPercentage;
    
    /**
     * Inventory information
     */
    @Field(type = FieldType.Boolean)
    private Boolean inStock;
    
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
    
    @Field(type = FieldType.Boolean)
    private Boolean lowStock;
    
    /**
     * Brand and manufacturer
     */
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Keyword)
    private String manufacturer;
    
    /**
     * Product attributes for faceted search
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Keyword)
    private List<String> colors;
    
    @Field(type = FieldType.Keyword)
    private List<String> sizes;
    
    @Field(type = FieldType.Keyword)
    private String material;
    
    /**
     * Review and rating information
     */
    @Field(type = FieldType.Float)
    private Float averageRating;
    
    @Field(type = FieldType.Integer)
    private Integer reviewCount;
    
    /**
     * Images for display
     */
    @Field(type = FieldType.Keyword)
    private List<String> imageUrls;
    
    @Field(type = FieldType.Keyword)
    private String primaryImageUrl;
    
    /**
     * SEO and marketing
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String metaDescription;
    
    @Field(type = FieldType.Keyword)
    private List<String> keywords;
    
    /**
     * Timestamps
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime lastIndexed;
    
    /**
     * Search optimization fields
     */
    @Field(type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "autocomplete_search")
    private String autocompleteText;
    
    @Field(type = FieldType.Text, analyzer = "synonym")
    private String searchableText;
    
    /**
     * Popularity and ranking signals
     */
    @Field(type = FieldType.Integer)
    private Integer viewCount;
    
    @Field(type = FieldType.Integer)
    private Integer orderCount;
    
    @Field(type = FieldType.Float)
    private Float popularityScore;
    
    @Field(type = FieldType.Float)
    private Float searchRankingScore;
    
    /**
     * Custom attributes for flexible filtering
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> customAttributes;
    
    /**
     * Variations and related products
     */
    @Field(type = FieldType.Keyword)
    private List<String> variationIds;
    
    @Field(type = FieldType.Keyword)
    private String parentProductId;
    
    /**
     * Promotional information
     */
    @Field(type = FieldType.Boolean)
    private Boolean isFeatured;
    
    @Field(type = FieldType.Boolean)
    private Boolean isNewArrival;
    
    @Field(type = FieldType.Boolean)
    private Boolean isBestSeller;
    
    /**
     * Geographic information (for future location-based search)
     */
    @GeoPointField
    private String location;
    
    /**
     * Status and visibility
     */
    @Field(type = FieldType.Boolean)
    private Boolean isActive;
    
    @Field(type = FieldType.Boolean)
    private Boolean isVisible;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    /**
     * Suggest fields for autocomplete functionality
     */
    @CompletionField(maxInputLength = 100)
    private Completion suggest;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Completion {
        private String[] input;
        private Integer weight;
        private Map<String, Object> contexts;
    }
}

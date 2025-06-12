package com.hungng3011.vdtecomberefresh.search.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for comprehensive search requests with full-text search capabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {
    
    /**
     * Main search query - supports full-text search
     */
    @Size(min = 1, max = 500, message = "Search query must be between 1 and 500 characters")
    private String query;
    
    /**
     * Search type specification
     */
    @Builder.Default
    private SearchType searchType = SearchType.FUZZY;
    
    /**
     * Filters to apply to the search
     */
    private SearchFilters filters;
    
    /**
     * Pagination parameters
     */
    @Min(value = 0, message = "Page must be 0 or greater")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "Size must be 1 or greater")
    @Builder.Default
    private Integer size = 20;
    
    /**
     * Sorting options
     */
    @Builder.Default
    private String sortBy = "relevance";
    
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;
    
    /**
     * Whether to include search suggestions
     */
    @Builder.Default
    private Boolean includeSuggestions = false;
    
    /**
     * Whether to include facets/aggregations
     */
    @Builder.Default
    private Boolean includeFacets = false;
    
    /**
     * Highlight configuration
     */
    @Builder.Default
    private Boolean highlightResults = true;
    
    /**
     * Search type enumeration
     */
    public enum SearchType {
        EXACT,          // Exact phrase matching
        FUZZY,          // Fuzzy matching with typo tolerance
        WILDCARD,       // Wildcard matching
        PHRASE_PREFIX,  // Phrase prefix matching for autocomplete
        MULTI_MATCH     // Multi-field matching
    }
    
    /**
     * Sort direction enumeration
     */
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Search filters
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFilters {
        
        /**
         * Category filter
         */
        private List<Long> categoryIds;
        
        /**
         * Price range filter
         */
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        
        /**
         * Brand filter
         */
        private List<String> brands;
        
        /**
         * Tags filter
         */
        private List<String> tags;
        
        /**
         * Availability filter
         */
        private Boolean inStock;
        
        /**
         * Rating filter
         */
        private Double minRating;
        
        /**
         * Date range filter
         */
        private String createdAfter;
        private String createdBefore;
        
        /**
         * Custom attributes filter
         */
        private Map<String, Object> customAttributes;
        
        /**
         * Location-based filter (for future use)
         */
        private LocationFilter location;
    }
    
    /**
     * Location filter for geo-spatial search
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationFilter {
        private Double latitude;
        private Double longitude;
        private String distance;
        private String unit;
    }
}

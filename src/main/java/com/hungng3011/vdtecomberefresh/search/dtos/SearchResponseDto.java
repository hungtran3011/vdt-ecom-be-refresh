package com.hungng3011.vdtecomberefresh.search.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for search response with comprehensive search results and metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto<T> {
    
    /**
     * Search results
     */
    private List<T> results;
    
    /**
     * Search metadata
     */
    private SearchMetadata metadata;
    
    /**
     * Search suggestions for typos/autocomplete
     */
    private List<SearchSuggestion> suggestions;
    
    /**
     * Faceted search results
     */
    private Map<String, List<FacetResult>> facets;
    
    /**
     * Search highlighting
     */
    private Map<String, List<String>> highlights;
    
    /**
     * Search metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private String query;
        private Long totalHits;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Double maxScore;
        private Long searchTime; // in milliseconds
        private Boolean hasNext;
        private Boolean hasPrevious;
        private String scrollId; // for deep pagination
    }
    
    /**
     * Search suggestion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSuggestion {
        private String text;
        private Double score;
        private SuggestionType type;
        private Long frequency;
        
        public enum SuggestionType {
            SPELL_CHECK,    // For typo corrections
            COMPLETION,     // For autocomplete
            PHRASE,         // For phrase suggestions
            TERM           // For term suggestions
        }
    }
    
    /**
     * Facet result for aggregated search
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetResult {
        private String key;
        private Long count;
        private Boolean selected;
        private Map<String, Object> metadata;
    }
}

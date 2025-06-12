package com.hungng3011.vdtecomberefresh.search.controllers;

import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchRequestDto;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchResponseDto;
import com.hungng3011.vdtecomberefresh.search.services.ProductSearchService;
import com.hungng3011.vdtecomberefresh.search.services.SearchAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for comprehensive product search functionality
 */
@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Search", description = "Comprehensive full-text search operations with advanced filtering and analytics")
public class ProductSearchController {
    
    private final ProductSearchService productSearchService;
    private final SearchAnalyticsService searchAnalyticsService;
    
    /**
     * Advanced product search with full-text search capabilities
     */
    @PostMapping("/products")
    @Operation(
        summary = "Advanced product search",
        description = "Perform comprehensive full-text search across products with advanced filtering, faceting, and suggestions"
    )
    public ResponseEntity<SearchResponseDto<ProductSearchDocument>> searchProducts(
            @Valid @RequestBody SearchRequestDto request) {
        
        log.info("Advanced search request: query='{}', type={}, filters={}", 
                request.getQuery(), request.getSearchType(), request.getFilters());
        
        try {
            SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
            
            log.info("Search completed: query='{}', results={}, time={}ms", 
                    request.getQuery(), 
                    response.getMetadata().getTotalHits(),
                    response.getMetadata().getSearchTime());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Search failed for query: '{}'", request.getQuery(), e);
            throw e;
        }
    }
    
    /**
     * Quick search for simple queries (backward compatibility)
     */
    @GetMapping("/products")
    @Operation(
        summary = "Quick product search",
        description = "Simple full-text search with basic parameters for backward compatibility"
    )
    public ResponseEntity<SearchResponseDto<ProductSearchDocument>> quickSearch(
            @Parameter(description = "Search query")
            @RequestParam(required = false) String q,
            
            @Parameter(description = "Search type")
            @RequestParam(defaultValue = "FUZZY") SearchRequestDto.SearchType type,
            
            @Parameter(description = "Category filter")
            @RequestParam(required = false) List<Long> categories,
            
            @Parameter(description = "Minimum price")
            @RequestParam(required = false) Double minPrice,
            
            @Parameter(description = "Maximum price")
            @RequestParam(required = false) Double maxPrice,
            
            @Parameter(description = "Brand filter")
            @RequestParam(required = false) List<String> brands,
            
            @Parameter(description = "In stock only")
            @RequestParam(defaultValue = "false") Boolean inStock,
            
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") Integer size,
            
            @Parameter(description = "Sort by")
            @RequestParam(defaultValue = "relevance") String sortBy,
            
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") SearchRequestDto.SortDirection sortDirection,
            
            @Parameter(description = "Include suggestions")
            @RequestParam(defaultValue = "false") Boolean suggestions,
            
            @Parameter(description = "Include facets")
            @RequestParam(defaultValue = "false") Boolean facets,
            
            @Parameter(description = "Highlight results")
            @RequestParam(defaultValue = "true") Boolean highlight) {
        
        log.info("Quick search: query='{}', type={}", q, type);
        
        // Build search request from parameters
        SearchRequestDto.SearchFilters filters = SearchRequestDto.SearchFilters.builder()
            .categoryIds(categories)
            .minPrice(minPrice != null ? java.math.BigDecimal.valueOf(minPrice) : null)
            .maxPrice(maxPrice != null ? java.math.BigDecimal.valueOf(maxPrice) : null)
            .brands(brands)
            .inStock(inStock)
            .build();
        
        SearchRequestDto request = SearchRequestDto.builder()
            .query(q)
            .searchType(type)
            .filters(filters)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .includeSuggestions(suggestions)
            .includeFacets(facets)
            .highlightResults(highlight)
            .build();
        
        return searchProducts(request);
    }
    
    /**
     * Get search suggestions for autocomplete
     */
    @GetMapping("/suggestions")
    @Operation(
        summary = "Get search suggestions",
        description = "Get autocomplete suggestions based on partial query input"
    )
    public ResponseEntity<List<SearchResponseDto.SearchSuggestion>> getSuggestions(
            @Parameter(description = "Partial query for suggestions")
            @RequestParam String q,
            
            @Parameter(description = "Maximum number of suggestions")
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("Getting suggestions for query: '{}'", q);
        
        try {
            List<SearchResponseDto.SearchSuggestion> suggestions = 
                productSearchService.getSuggestions(q, limit);
            
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            log.error("Failed to get suggestions for query: '{}'", q, e);
            throw e;
        }
    }
    
    /**
     * Get search facets for filtering UI
     */
    @PostMapping("/facets")
    @Operation(
        summary = "Get search facets",
        description = "Get faceted search results for building filter UI components"
    )
    public ResponseEntity<Map<String, List<SearchResponseDto.FacetResult>>> getFacets(
            @Valid @RequestBody SearchRequestDto request) {
        
        log.debug("Getting facets for search request");
        
        try {
            Map<String, List<SearchResponseDto.FacetResult>> facets = 
                productSearchService.getFacets(request);
            
            return ResponseEntity.ok(facets);
            
        } catch (Exception e) {
            log.error("Failed to get facets", e);
            throw e;
        }
    }
    
    /**
     * Get popular search queries
     */
    @GetMapping("/popular")
    @Operation(
        summary = "Get popular search queries",
        description = "Get the most popular search queries for trending searches"
    )
    public ResponseEntity<List<String>> getPopularQueries(
            @Parameter(description = "Maximum number of queries to return")
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("Getting popular queries");
        
        try {
            List<String> popularQueries = searchAnalyticsService.getPopularQueries(limit);
            return ResponseEntity.ok(popularQueries);
            
        } catch (Exception e) {
            log.error("Failed to get popular queries", e);
            throw e;
        }
    }
    
    /**
     * Log search result click for analytics
     */
    @PostMapping("/click")
    @Operation(
        summary = "Log search result click",
        description = "Track user clicks on search results for analytics and ranking improvement"
    )
    public ResponseEntity<Void> logClick(
            @Parameter(description = "Original search query")
            @RequestParam String query,
            
            @Parameter(description = "Clicked product ID")
            @RequestParam String productId,
            
            @Parameter(description = "Position in search results")
            @RequestParam Integer position) {
        
        log.debug("Logging click: query='{}', productId='{}', position={}", 
                query, productId, position);
        
        try {
            searchAnalyticsService.logSearchResultClick(query, productId, position);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Failed to log search result click", e);
            return ResponseEntity.status(HttpStatus.SC_ACCEPTED).build(); // signal partial failure
         }
    }
    
    /**
     * Get search analytics dashboard (admin only)
     */
    @GetMapping("/analytics/dashboard")
    @Operation(
        summary = "Get search analytics dashboard",
        description = "Get comprehensive search analytics data for admin dashboard"
    )
    public ResponseEntity<Map<String, Object>> getSearchDashboard() {
        
        log.debug("Getting search analytics dashboard");
        
        try {
            Map<String, Object> dashboard = searchAnalyticsService.getSearchDashboard();
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("Failed to get search analytics dashboard", e);
            throw e;
        }
    }
    
    /**
     * Get failed search queries (admin only)
     */
    @GetMapping("/analytics/failed")
    @Operation(
        summary = "Get failed search queries",
        description = "Get search queries that returned no results for content optimization"
    )
    public ResponseEntity<List<String>> getFailedQueries(
            @Parameter(description = "Maximum number of queries to return")
            @RequestParam(defaultValue = "20") Integer limit) {
        
        log.debug("Getting failed queries");
        
        try {
            List<String> failedQueries = searchAnalyticsService.getFailedQueries(limit);
            return ResponseEntity.ok(failedQueries);
            
        } catch (Exception e) {
            log.error("Failed to get failed queries", e);
            throw e;
        }
    }
    
    /**
     * Get search statistics for a specific date (admin only)
     */
    @GetMapping("/analytics/stats/{date}")
    @Operation(
        summary = "Get search statistics",
        description = "Get search performance statistics for a specific date"
    )
    public ResponseEntity<Map<String, Object>> getSearchStats(
            @Parameter(description = "Date in yyyy-MM-dd format")
            @PathVariable String date) {
        
        log.debug("Getting search stats for date: {}", date);
        
        try {
            Map<String, Object> stats = searchAnalyticsService.getDailyStats(date);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get search stats for date: {}", date, e);
            throw e;
        }
    }
    
    /**
     * Health check for search service
     */
    @GetMapping("/health")
    @Operation(
        summary = "Search service health check",
        description = "Check the health status of the search service and Elasticsearch"
    )
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            // Perform a simple search to test Elasticsearch connectivity
            SearchRequestDto testRequest = SearchRequestDto.builder()
                .query("test")
                .page(0)
                .size(1)
                .build();
            
            productSearchService.search(testRequest);
            
            Map<String, String> health = Map.of(
                "status", "UP",
                "service", "product-search",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Search service health check failed", e);
            
            Map<String, String> health = Map.of(
                "status", "DOWN",
                "service", "product-search",
                "error", e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.status(503).body(health);
        }
    }
}

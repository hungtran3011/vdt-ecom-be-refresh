package com.hungng3011.vdtecomberefresh.search.services;

import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchRequestDto;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchResponseDto;
import com.hungng3011.vdtecomberefresh.search.exceptions.SearchServiceUnavailableException;
import com.hungng3011.vdtecomberefresh.search.exceptions.SearchValidationException;
import com.hungng3011.vdtecomberefresh.search.repositories.ProductSearchRepository;
import com.hungng3011.vdtecomberefresh.search.utils.SearchConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced product search service with validation and error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {
    
    private final ProductSearchRepository productSearchRepository;
    private final SearchAnalyticsService searchAnalyticsService;
    
    /**
     * Perform comprehensive product search with validation
     */
    public SearchResponseDto<ProductSearchDocument> search(SearchRequestDto request) {
        // Validate request
        validateSearchRequest(request);
        
        // Sanitize query
        String sanitizedQuery = SearchConstants.sanitizeQuery(request.getQuery());
        request.setQuery(sanitizedQuery);
        
        log.info("Performing search with query: '{}', type: {}", request.getQuery(), request.getSearchType());
        
        long startTime = System.currentTimeMillis();
        
        try {
            Page<ProductSearchDocument> results = performSearch(request);
            
            // Build response
            SearchResponseDto<ProductSearchDocument> response = buildSearchResponse(
                results, 
                request, 
                System.currentTimeMillis() - startTime
            );
            
            // Log search analytics
            searchAnalyticsService.logSearch(request, response);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error performing search for query: '{}'", request.getQuery(), e);
            throw new SearchServiceUnavailableException("Search service temporarily unavailable", e);
        }
    }
    
    /**
     * Get search suggestions with validation
     */
    public List<SearchResponseDto.SearchSuggestion> getSuggestions(String query, int limit) {
        // Validate suggestion request
        SearchConstants.validateSuggestionRequest(query, limit);
        
        String sanitizedQuery = SearchConstants.sanitizeQuery(query);
        log.info("Getting suggestions for query: '{}'", sanitizedQuery);
        
        try {
            Pageable pageable = PageRequest.of(0, Math.min(limit, SearchConstants.MAX_SUGGESTION_LIMIT));
            Page<ProductSearchDocument> suggestions = productSearchRepository.findByAutocompleteTextContaining(sanitizedQuery, pageable);
            
            return suggestions.stream()
                .map(doc -> SearchResponseDto.SearchSuggestion.builder()
                    .text(doc.getName())
                    .score(1.0) // Fixed score for simplified version
                    .type(SearchResponseDto.SearchSuggestion.SuggestionType.COMPLETION)
                    .frequency(1L)
                    .build())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting suggestions for query: '{}'", sanitizedQuery, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get search facets for filtering
     */
    public Map<String, List<SearchResponseDto.FacetResult>> getFacets(SearchRequestDto request) {
        log.info("Getting facets for search request");
        
        try {
            Map<String, List<SearchResponseDto.FacetResult>> facets = new HashMap<>();
            
            // Category facets
            List<Object[]> categoryFacets = productSearchRepository.findCategoryFacets();
            facets.put("categories", categoryFacets.stream()
                .map(row -> SearchResponseDto.FacetResult.builder()
                    .key(row[0].toString())
                    .count(((Number) row[1]).longValue())
                    .selected(false)
                    .build())
                .collect(Collectors.toList()));
            
            // Brand facets
            List<Object[]> brandFacets = productSearchRepository.findBrandFacets();
            facets.put("brands", brandFacets.stream()
                .map(row -> SearchResponseDto.FacetResult.builder()
                    .key(row[0].toString())
                    .count(((Number) row[1]).longValue())
                    .selected(false)
                    .build())
                .collect(Collectors.toList()));
            
            return facets;
            
        } catch (Exception e) {
            log.error("Error getting facets", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Perform the actual search based on request parameters
     */
    private Page<ProductSearchDocument> performSearch(SearchRequestDto request) {
        Pageable pageable = createPageable(request);
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return applyFilters(productSearchRepository.findAll(pageable), request);
        }
        
        String query = request.getQuery().trim();
        Page<ProductSearchDocument> results;
        
        switch (request.getSearchType()) {
            case EXACT -> results = productSearchRepository.findByNameContaining(query, pageable);
            case FUZZY -> results = productSearchRepository.findBySearchableTextContaining(query, pageable);
            case WILDCARD -> results = productSearchRepository.findBySearchableTextContaining(query, pageable);
            case PHRASE_PREFIX -> results = productSearchRepository.findByAutocompleteTextContaining(query, pageable);
            case MULTI_MATCH -> results = productSearchRepository.findByMultipleFields(query, pageable);
            default -> results = productSearchRepository.findBySearchableTextContaining(query, pageable);
        }
        
        return applyFilters(results, request);
    }
    
    /**
     * Apply filters to search results
     */
    private Page<ProductSearchDocument> applyFilters(Page<ProductSearchDocument> results, SearchRequestDto request) {
        if (request.getFilters() == null) {
            return results;
        }
        
        // Note: This is a simplified implementation. In a real scenario, 
        // you'd want to apply filters at the database/Elasticsearch level for better performance
        return results; // Return original for now
    }
    
    /**
     * Create pageable with sorting
     */
    private Pageable createPageable(SearchRequestDto request) {
        Sort sort = createSort(request);
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
    
    /**
     * Create sorting options
     */
    private Sort createSort(SearchRequestDto request) {
        String sortBy = request.getSortBy();
        Sort.Direction direction = request.getSortDirection() == SearchRequestDto.SortDirection.DESC 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        return switch (sortBy.toLowerCase()) {
            case "price" -> Sort.by(direction, "basePrice");
            case "name" -> Sort.by(direction, "name");
            case "rating" -> Sort.by(direction, "averageRating");
            case "popularity" -> Sort.by(direction, "popularityScore");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "searchRankingScore");
        };
    }
    
    /**
     * Build the final search response
     */
    private SearchResponseDto<ProductSearchDocument> buildSearchResponse(
        Page<ProductSearchDocument> results, 
        SearchRequestDto request, 
        long searchTime) {
        
        SearchResponseDto.SearchMetadata metadata = SearchResponseDto.SearchMetadata.builder()
            .query(request.getQuery())
            .totalHits(results.getTotalElements())
            .page(request.getPage())
            .size(request.getSize())
            .totalPages(results.getTotalPages())
            .maxScore(1.0) // Simplified - not available in Page interface
            .searchTime(searchTime)
            .hasNext(results.hasNext())
            .hasPrevious(results.hasPrevious())
            .build();
        
        SearchResponseDto.SearchResponseDtoBuilder<ProductSearchDocument> responseBuilder = SearchResponseDto.<ProductSearchDocument>builder()
            .results(results.getContent())
            .metadata(metadata);
        
        // Add suggestions if requested
        if (request.getIncludeSuggestions()) {
            responseBuilder.suggestions(getSuggestions(request.getQuery(), 5));
        }
        
        // Add facets if requested
        if (request.getIncludeFacets()) {
            responseBuilder.facets(getFacets(request));
        }
        
        // Add empty highlights for now
        responseBuilder.highlights(new HashMap<>());
        
        return responseBuilder.build();
    }
    
    /**
     * Validate search request
     */
    private void validateSearchRequest(SearchRequestDto request) {
        if (request == null) {
            throw new SearchValidationException("Search request cannot be null");
        }
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new SearchValidationException("Search query cannot be null or empty");
        }
        
        if (request.getPage() < 0) {
            throw new SearchValidationException("Page number cannot be negative");
        }
        
        if (request.getSize() <= 0) {
            throw new SearchValidationException("Page size must be greater than zero");
        }
    }
}

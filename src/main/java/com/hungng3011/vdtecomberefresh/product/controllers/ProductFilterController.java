package com.hungng3011.vdtecomberefresh.product.controllers;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.dtos.filters.ProductFilterDto;
import com.hungng3011.vdtecomberefresh.product.services.ProductFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for product filtering operations with comprehensive security protection.
 * Provides secure filtering, searching, and statistical endpoints for products.
 */
@RestController
@RequestMapping("/v1/products/filter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Filter", description = "Secure product filtering and search operations")
public class ProductFilterController {
    
    private final ProductFilterService productFilterService;

    /**
     * Filter products with advanced criteria and security protection
     */
    @PostMapping("/search")
    @Operation(
        summary = "Filter products with advanced criteria",
        description = "Secure filtering with SQL injection protection, input validation, and pagination"
    )
    public ResponseEntity<PagedResponse<ProductDto>> filterProducts(
            @Valid @RequestBody ProductFilterDto filterDto) {
        
        log.info("Filtering products with criteria: name={}, category={}, priceRange=[{}-{}], page={}, size={}", 
                filterDto.getName(), filterDto.getCategoryId(), 
                filterDto.getMinPrice(), filterDto.getMaxPrice(),
                filterDto.getPage(), filterDto.getSize());
        
        try {
            PagedResponse<ProductDto> response = productFilterService.filterProducts(filterDto);
            
            log.info("Successfully filtered products - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for products: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error filtering products with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Get product statistics based on filter criteria
     */
    @PostMapping("/statistics")
    @Operation(
        summary = "Get product statistics",
        description = "Retrieve statistical information about filtered products with security protection"
    )
    public ResponseEntity<Map<String, Object>> getProductStatistics(
            @Valid @RequestBody ProductFilterDto filterDto) {
        
        log.info("Getting product statistics with criteria: name={}, category={}, priceRange=[{}-{}]", 
                filterDto.getName(), filterDto.getCategoryId(), 
                filterDto.getMinPrice(), filterDto.getMaxPrice());
        
        try {
            Map<String, Object> statistics = productFilterService.getProductStatistics(filterDto);
            
            log.info("Successfully retrieved product statistics");
            
            return ResponseEntity.ok(statistics);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter criteria for product statistics: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting product statistics with criteria: {}", filterDto, e);
            throw e;
        }
    }

    /**
     * Search products by text with security protection
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search products by text",
        description = "Secure text-based product search with SQL injection protection"
    )
    public ResponseEntity<PagedResponse<ProductDto>> searchProducts(
            @Parameter(description = "Search query (sanitized for security)")
            @RequestParam(required = false) String query,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (name, price, category)")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Searching products with query='{}', page={}, size={}, sortBy={}, sortDir={}", 
                query, page, size, sortBy, sortDir);
        
        try {
            ProductFilterDto filterDto = ProductFilterDto.builder()
                    .name(query)
                    .page(page)
                    .size(size)
                    .sortBy(mapSortField(sortBy))
                    .sortDirection(mapSortDirection(sortDir))
                    .build();
            
            PagedResponse<ProductDto> response = productFilterService.filterProducts(filterDto);
            
            log.info("Successfully searched products - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search parameters for products: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error searching products with query '{}': {}", query, e.getMessage());
            throw e;
        }
    }

    /**
     * Get products by category with filtering
     */
    @GetMapping("/category/{categoryId}")
    @Operation(
        summary = "Get products by category with filtering",
        description = "Retrieve products from specific category with additional filtering options"
    )
    public ResponseEntity<PagedResponse<ProductDto>> getProductsByCategory(
            @Parameter(description = "Category ID")
            @PathVariable Long categoryId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (name, price)")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Getting products by category {} with filters: priceRange=[{}-{}], page={}, size={}", 
                categoryId, minPrice, maxPrice, page, size);
        
        try {
            ProductFilterDto filterDto = ProductFilterDto.builder()
                    .categoryId(categoryId)
                    .minPrice(convertToBigDecimal(minPrice))
                    .maxPrice(convertToBigDecimal(maxPrice))
                    .page(page)
                    .size(size)
                    .sortBy(mapSortField(sortBy))
                    .sortDirection(mapSortDirection(sortDir))
                    .build();
            
            PagedResponse<ProductDto> response = productFilterService.filterProducts(filterDto);
            
            log.info("Successfully retrieved products by category - found {} items on page {} of {}", 
                    response.getContent().size(), response.getPagination().getPage(), response.getPagination().getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for category products: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting products by category {}: {}", categoryId, e.getMessage());
            throw e;
        }
    }

    /**
     * Health check endpoint for product filter service
     */
    @GetMapping("/health")
    @Operation(summary = "Product filter service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "product-filter",
            "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Map string to ProductSortField enum safely
     */
    private ProductFilterDto.ProductSortField mapSortField(String sortBy) {
        if (sortBy == null) return ProductFilterDto.ProductSortField.ID;
        
        try {
            switch (sortBy.toLowerCase()) {
                case "name":
                    return ProductFilterDto.ProductSortField.NAME;
                case "price":
                case "baseprice":
                case "base_price":
                    return ProductFilterDto.ProductSortField.BASE_PRICE;
                case "created":
                case "created_at":
                    return ProductFilterDto.ProductSortField.CREATED_AT;
                default:
                    return ProductFilterDto.ProductSortField.ID;
            }
        } catch (Exception e) {
            return ProductFilterDto.ProductSortField.ID;
        }
    }
    
    /**
     * Map string to SortDirection enum safely
     */
    private ProductFilterDto.SortDirection mapSortDirection(String sortDir) {
        if (sortDir == null) return ProductFilterDto.SortDirection.ASC;
        
        try {
            return sortDir.toLowerCase().startsWith("desc") ? 
                ProductFilterDto.SortDirection.DESC : 
                ProductFilterDto.SortDirection.ASC;
        } catch (Exception e) {
            return ProductFilterDto.SortDirection.ASC;
        }
    }
    
    /**
     * Convert Double to BigDecimal safely
     */
    private java.math.BigDecimal convertToBigDecimal(Double value) {
        return value != null ? java.math.BigDecimal.valueOf(value) : null;
    }
}

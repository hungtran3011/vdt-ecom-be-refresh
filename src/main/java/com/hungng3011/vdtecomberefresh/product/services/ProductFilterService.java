package com.hungng3011.vdtecomberefresh.product.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.dtos.filters.ProductFilterDto;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.mappers.ProductMapper;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for secure product filtering with SQL injection protection
 * Uses parameterized queries and input validation to prevent security vulnerabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductFilterService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    
    /**
     * Filter products using comprehensive criteria with SQL injection protection
     * 
     * @param filterDto Filter criteria with validated input
     * @return Paginated and filtered product results
     */
    public PagedResponse<ProductDto> filterProducts(ProductFilterDto filterDto) {
        log.info("Filtering products with criteria: {}", filterDto);
        
        try {
            // Validate and sanitize input
            ProductFilterDto sanitizedFilter = sanitizeFilterDto(filterDto);
            
            // Create pageable with secure sorting
            Pageable pageable = createSecurePageable(sanitizedFilter);
            
            Page<Product> productPage;
            
            // Apply filtering based on complexity
            if (hasMultipleCriteria(sanitizedFilter)) {
                productPage = applyComprehensiveFilter(sanitizedFilter, pageable);
            } else if (hasBasicCriteria(sanitizedFilter)) {
                productPage = applyBasicFilter(sanitizedFilter, pageable);
            } else {
                // Default: return all products with pagination
                productPage = productRepository.findAll(pageable);
            }
            
            // Convert to DTOs
            List<ProductDto> productDtos = productPage.getContent()
                    .stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());
            
            // Build response with pagination metadata
            return buildPagedResponse(productDtos, productPage, sanitizedFilter);
            
        } catch (Exception e) {
            log.error("Error filtering products with criteria: {}", filterDto, e);
            throw new RuntimeException("Failed to filter products", e);
        }
    }
    
    /**
     * Sanitize filter input to prevent injection attacks
     */
    private ProductFilterDto sanitizeFilterDto(ProductFilterDto filterDto) {
        if (filterDto == null) {
            return new ProductFilterDto();
        }
        
        ProductFilterDto sanitized = new ProductFilterDto();
        
        // Sanitize string fields - remove potential SQL injection patterns
        sanitized.setName(sanitizeStringInput(filterDto.getName()));
        sanitized.setDescription(sanitizeStringInput(filterDto.getDescription()));
        
        // Copy validated numeric and enum fields
        sanitized.setCategoryId(filterDto.getCategoryId());
        sanitized.setMinPrice(filterDto.getMinPrice());
        sanitized.setMaxPrice(filterDto.getMaxPrice());
        sanitized.setSortBy(filterDto.getSortBy() != null ? filterDto.getSortBy() : ProductFilterDto.ProductSortField.ID);
        sanitized.setSortDirection(filterDto.getSortDirection() != null ? filterDto.getSortDirection() : ProductFilterDto.SortDirection.ASC);
        sanitized.setPage(filterDto.getPage() != null ? Math.max(0, filterDto.getPage()) : 0);
        sanitized.setSize(filterDto.getSize() != null ? Math.min(100, Math.max(1, filterDto.getSize())) : 20);
        
        // Sanitize dynamic fields
        if (filterDto.getDynamicFields() != null && !filterDto.getDynamicFields().isEmpty()) {
            List<ProductFilterDto.DynamicFieldFilterDto> sanitizedDynamicFields = filterDto.getDynamicFields()
                    .stream()
                    .limit(10) // Limit to prevent abuse
                    .map(this::sanitizeDynamicField)
                    .collect(Collectors.toList());
            sanitized.setDynamicFields(sanitizedDynamicFields);
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize string input to remove potentially dangerous characters
     */
    private String sanitizeStringInput(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        
        // Remove potentially dangerous SQL injection patterns
        String sanitized = input.trim()
                .replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", "")
                .replaceAll("[';\"\\\\]", ""); // Remove quotes and backslashes
        
        return sanitized.length() > 0 ? sanitized : null;
    }
    
    /**
     * Sanitize dynamic field filter
     */
    private ProductFilterDto.DynamicFieldFilterDto sanitizeDynamicField(ProductFilterDto.DynamicFieldFilterDto field) {
        ProductFilterDto.DynamicFieldFilterDto sanitized = new ProductFilterDto.DynamicFieldFilterDto();
        sanitized.setFieldName(sanitizeStringInput(field.getFieldName()));
        sanitized.setValue(sanitizeStringInput(field.getValue()));
        sanitized.setMatchType(field.getMatchType() != null ? field.getMatchType() : ProductFilterDto.FieldMatchType.EQUALS);
        return sanitized;
    }
    
    /**
     * Create secure pageable with sorting validation
     */
    private Pageable createSecurePageable(ProductFilterDto filterDto) {
        // Map enum to actual entity field names
        String sortField = mapSortField(filterDto.getSortBy());
        Sort.Direction direction = filterDto.getSortDirection() == ProductFilterDto.SortDirection.DESC 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        
        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(filterDto.getPage(), filterDto.getSize(), sort);
    }
    
    /**
     * Map sort field enum to actual entity field names - prevents field injection
     */
    private String mapSortField(ProductFilterDto.ProductSortField sortField) {
        switch (sortField) {
            case ID:
                return "id";
            case NAME:
                return "name";
            case BASE_PRICE:
                return "basePrice";
            case CREATED_AT:
                return "id"; // Use id as proxy for creation time
            default:
                return "id";
        }
    }
    
    /**
     * Check if filter has multiple criteria requiring comprehensive search
     */
    private boolean hasMultipleCriteria(ProductFilterDto filterDto) {
        return (filterDto.getDynamicFields() != null && !filterDto.getDynamicFields().isEmpty()) ||
               (StringUtils.hasText(filterDto.getName()) && 
                (filterDto.getCategoryId() != null || filterDto.getMinPrice() != null || filterDto.getMaxPrice() != null));
    }
    
    /**
     * Check if filter has basic criteria
     */
    private boolean hasBasicCriteria(ProductFilterDto filterDto) {
        return StringUtils.hasText(filterDto.getName()) ||
               StringUtils.hasText(filterDto.getDescription()) ||
               filterDto.getCategoryId() != null ||
               filterDto.getMinPrice() != null ||
               filterDto.getMaxPrice() != null;
    }
    
    /**
     * Apply comprehensive filter with dynamic fields
     */
    private Page<Product> applyComprehensiveFilter(ProductFilterDto filterDto, Pageable pageable) {
        // For now, handle the first dynamic field - can be extended for multiple fields
        ProductFilterDto.DynamicFieldFilterDto dynamicField = filterDto.getDynamicFields() != null 
                && !filterDto.getDynamicFields().isEmpty() 
                ? filterDto.getDynamicFields().get(0) 
                : null;
        
        String fieldName = dynamicField != null ? dynamicField.getFieldName() : null;
        String fieldValue = dynamicField != null ? dynamicField.getValue() : null;
        String matchType = dynamicField != null ? dynamicField.getMatchType().name() : "EQUALS";
        
        return productRepository.findByComprehensiveCriteria(
                filterDto.getName(),
                filterDto.getDescription(),
                filterDto.getCategoryId(),
                filterDto.getMinPrice(),
                filterDto.getMaxPrice(),
                fieldName,
                fieldValue,
                matchType,
                pageable);
    }
    
    /**
     * Apply basic filter without dynamic fields
     */
    private Page<Product> applyBasicFilter(ProductFilterDto filterDto, Pageable pageable) {
        return productRepository.findByMultipleCriteria(
                filterDto.getName(),
                filterDto.getDescription(),
                filterDto.getCategoryId(),
                filterDto.getMinPrice(),
                filterDto.getMaxPrice(),
                pageable);
    }
    
    /**
     * Build paged response with metadata
     */
    private PagedResponse<ProductDto> buildPagedResponse(List<ProductDto> content, Page<Product> page, ProductFilterDto filterDto) {
        PagedResponse.PaginationMetadata metadata = PagedResponse.PaginationMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
        
        return new PagedResponse<>(content, metadata);
    }
    
    /**
     * Get product statistics based on filter criteria
     */
    public Map<String, Object> getProductStatistics(ProductFilterDto filterDto) {
        log.info("Getting product statistics with criteria: {}", filterDto);
        
        try {
            // Validate and sanitize input
            ProductFilterDto sanitizedFilter = sanitizeFilterDto(filterDto);
            
            // Get filtered products count
            long totalCount;
            
            if (hasMultipleCriteria(sanitizedFilter)) {
                // For comprehensive criteria, we need to extract dynamic field info
                ProductFilterDto.DynamicFieldFilterDto dynamicField = sanitizedFilter.getDynamicFields() != null 
                        && !sanitizedFilter.getDynamicFields().isEmpty() 
                        ? sanitizedFilter.getDynamicFields().get(0) 
                        : null;
                
                String fieldName = dynamicField != null ? dynamicField.getFieldName() : null;
                String fieldValue = dynamicField != null ? dynamicField.getValue() : null;
                String matchType = dynamicField != null ? dynamicField.getMatchType().name() : "EQUALS";
                
                totalCount = productRepository.countByComprehensiveCriteria(
                        sanitizedFilter.getName(),
                        sanitizedFilter.getDescription(),
                        sanitizedFilter.getCategoryId(),
                        sanitizedFilter.getMinPrice(),
                        sanitizedFilter.getMaxPrice(),
                        fieldName,
                        fieldValue,
                        matchType);
            } else if (hasBasicCriteria(sanitizedFilter)) {
                totalCount = productRepository.countByMultipleCriteria(
                        sanitizedFilter.getName(),
                        sanitizedFilter.getDescription(),
                        sanitizedFilter.getCategoryId(),
                        sanitizedFilter.getMinPrice(),
                        sanitizedFilter.getMaxPrice());
            } else {
                totalCount = productRepository.count();
            }
            
            // Return basic statistics
            Map<String, Object> statistics = new java.util.HashMap<>();
            statistics.put("totalCount", totalCount);
            statistics.put("hasFilters", hasBasicCriteria(sanitizedFilter) || hasMultipleCriteria(sanitizedFilter));
            statistics.put("filterCriteria", Map.of(
                "hasNameFilter", sanitizedFilter.getName() != null,
                "hasCategoryFilter", sanitizedFilter.getCategoryId() != null,
                "hasPriceFilter", sanitizedFilter.getMinPrice() != null || sanitizedFilter.getMaxPrice() != null
            ));
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting product statistics with criteria: {}", filterDto, e);
            throw new RuntimeException("Failed to get product statistics", e);
        }
    }
}

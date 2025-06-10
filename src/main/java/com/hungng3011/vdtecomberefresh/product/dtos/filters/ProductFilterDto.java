package com.hungng3011.vdtecomberefresh.product.dtos.filters;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for filtering products with comprehensive search criteria
 * and SQL injection protection through validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDto {
    
    @Size(max = 255, message = "Name filter must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description filter must not exceed 1000 characters")
    private String description;
    
    @Min(value = 1, message = "Category ID must be positive")
    private Long categoryId;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum price must be non-negative")
    private BigDecimal minPrice;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum price must be non-negative")
    private BigDecimal maxPrice;
    
    // Dynamic field filtering with validation
    @Size(max = 10, message = "Maximum 10 dynamic field filters allowed")
    private List<DynamicFieldFilterDto> dynamicFields;
    
    // Sorting options
    @Builder.Default
    private ProductSortField sortBy = ProductSortField.ID;
    @Builder.Default
    private SortDirection sortDirection = SortDirection.ASC;
    
    // Pagination
    @Min(value = 0, message = "Page must be non-negative")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "Size must be positive")
    @Builder.Default
    private Integer size = 20;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicFieldFilterDto {
        @Size(max = 100, message = "Field name must not exceed 100 characters")
        private String fieldName;
        
        @Size(max = 500, message = "Field value must not exceed 500 characters")
        private String value;
        
        private FieldMatchType matchType = FieldMatchType.EQUALS;
    }
    
    public enum ProductSortField {
        ID, NAME, BASE_PRICE, CREATED_AT
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    public enum FieldMatchType {
        EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH
    }
}

# E-Commerce Backend - Pagination and Filtering System Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Security Features](#security-features)
4. [API Endpoints](#api-endpoints)
5. [Filter DTOs](#filter-dtos)
6. [Pagination](#pagination)
7. [Usage Examples](#usage-examples)
8. [Error Handling](#error-handling)
9. [Performance Considerations](#performance-considerations)
10. [Testing](#testing)

## Overview

The VDT E-Commerce Backend implements a comprehensive filtering and pagination system with robust security protections. The system provides advanced search capabilities across Products, Orders, and Payments with SQL injection protection, input validation, and optimized query performance.

### Key Features
- **Secure Filtering**: SQL injection protection through parameterized queries
- **Advanced Search**: Multiple criteria, date ranges, and dynamic field filtering
- **Pagination**: Efficient cursor-based and offset-based pagination
- **Input Validation**: Comprehensive validation using Jakarta Bean Validation
- **Statistics**: Aggregated data and analytics endpoints
- **Sorting**: Flexible sorting options with multiple fields
- **Performance**: Optimized queries with proper indexing

## Architecture

### Component Structure
```
filtering-system/
├── controllers/
│   ├── ProductFilterController
│   ├── OrderFilterController
│   └── PaymentFilterController
├── services/
│   ├── ProductFilterService
│   ├── OrderFilterService
│   └── PaymentFilterService
├── dtos/filters/
│   ├── ProductFilterDto
│   ├── OrderFilterDto
│   └── PaymentFilterDto
└── repositories/
    ├── ProductRepository (enhanced)
    ├── OrderRepository (enhanced)
    └── PaymentRepository (enhanced)
```

### Design Patterns
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Data transfer with validation
- **Builder Pattern**: Flexible filter construction
- **Service Layer**: Business logic separation

## Security Features

### SQL Injection Protection
All filtering operations use parameterized queries with prepared statements:

```java
// Example from ProductFilterService
@Query("SELECT p FROM Product p WHERE " +
       "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
       "(:categoryId IS NULL OR p.category.id = :categoryId)")
Page<Product> findByMultipleCriteria(
    @Param("name") String name,
    @Param("categoryId") Long categoryId,
    Pageable pageable
);
```

### Input Validation
Comprehensive validation using Jakarta Bean Validation:

```java
@Size(max = 255, message = "Name filter must not exceed 255 characters")
private String name;

@DecimalMin(value = "0.0", inclusive = true, message = "Minimum price must be non-negative")
private BigDecimal minPrice;

@Min(value = 0, message = "Page must be non-negative")
@Builder.Default
private Integer page = 0;
```

### Rate Limiting
- Input size limits prevent excessive resource consumption
- Maximum filter criteria limits (e.g., max 10 dynamic fields)
- Pagination size limits (max 100 items per page)

## API Endpoints

### Product Filtering

#### Filter Products
```http
POST /v1/products/filter/search
Content-Type: application/json

{
  "name": "laptop",
  "categoryId": 1,
  "minPrice": 500.00,
  "maxPrice": 2000.00,
  "dynamicFields": [
    {
      "fieldName": "brand",
      "value": "Dell",
      "matchType": "EQUALS"
    }
  ],
  "sortBy": "NAME",
  "sortDirection": "ASC",
  "page": 0,
  "size": 20
}
```

#### Get Product Statistics
```http
GET /v1/products/filter/statistics?categoryId=1&minPrice=100&maxPrice=1000
```

#### Quick Search Products
```http
GET /v1/products/filter/quick-search?query=gaming%20laptop&page=0&size=10
```

#### Export Products
```http
POST /v1/products/filter/export
Content-Type: application/json

{
  "name": "laptop",
  "categoryId": 1,
  "format": "CSV"
}
```

### Order Filtering

#### Filter Orders
```http
POST /v1/orders/filter/search
Content-Type: application/json

{
  "userId": "user123",
  "orderStatuses": ["CONFIRMED", "SHIPPED"],
  "paymentStatuses": ["PAID"],
  "paymentMethods": ["VIETTEL_MONEY"],
  "minTotalPrice": 100.00,
  "maxTotalPrice": 1000.00,
  "createdAfter": "2024-01-01T00:00:00",
  "createdBefore": "2024-12-31T23:59:59",
  "sortBy": "CREATED_AT",
  "sortDirection": "DESC",
  "page": 0,
  "size": 20
}
```

#### Get Order Statistics
```http
GET /v1/orders/filter/statistics?userId=user123&status=CONFIRMED
```

#### Get Orders by Date Range
```http
GET /v1/orders/filter/date-range?startDate=2024-01-01&endDate=2024-12-31&page=0&size=10
```

### Payment Filtering

#### Filter Payments
```http
POST /v1/payments/filter/search
Content-Type: application/json

{
  "userId": "user123",
  "paymentStatuses": ["PAID", "CONFIRMED"],
  "paymentMethods": ["VIETTEL_MONEY"],
  "minAmount": 50.00,
  "maxAmount": 500.00,
  "createdAfter": "2024-01-01T00:00:00",
  "createdBefore": "2024-12-31T23:59:59",
  "dateRangePreset": "LAST_30_DAYS",
  "sortBy": "CREATED_AT",
  "sortDirection": "DESC",
  "page": 0,
  "size": 20
}
```

#### Get Payment Statistics
```http
GET /v1/payments/filter/statistics?userId=user123&method=VIETTEL_MONEY
```

#### Get Payment Summary
```http
GET /v1/payments/filter/summary?period=MONTHLY&year=2024
```

## Filter DTOs

### ProductFilterDto
```java
public class ProductFilterDto {
    @Size(max = 255)
    private String name;
    
    @Size(max = 1000)
    private String description;
    
    @Min(1)
    private Long categoryId;
    
    @DecimalMin("0.0")
    private BigDecimal minPrice;
    
    @DecimalMin("0.0")
    private BigDecimal maxPrice;
    
    @Size(max = 10)
    private List<DynamicFieldFilterDto> dynamicFields;
    
    @Builder.Default
    private ProductSortField sortBy = ProductSortField.ID;
    
    @Builder.Default
    private SortDirection sortDirection = SortDirection.ASC;
    
    @Min(0) @Builder.Default
    private Integer page = 0;
    
    @Min(1) @Builder.Default
    private Integer size = 20;
}
```

### OrderFilterDto
```java
public class OrderFilterDto {
    @Size(max = 50)
    private String userId;
    
    private List<OrderStatus> orderStatuses;
    private List<PaymentStatus> paymentStatuses;
    private List<PaymentMethod> paymentMethods;
    
    @Size(max = 100)
    private String phone;
    
    @Size(max = 500)
    private String address;
    
    @DecimalMin("0.0")
    private BigDecimal minTotalPrice;
    
    @DecimalMin("0.0")
    private BigDecimal maxTotalPrice;
    
    @Past
    private LocalDateTime createdAfter;
    
    @Past
    private LocalDateTime createdBefore;
    
    @Builder.Default
    private OrderSortField sortBy = OrderSortField.CREATED_AT;
    
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;
    
    @Min(0) @Builder.Default
    private Integer page = 0;
    
    @Min(1) @Builder.Default
    private Integer size = 20;
}
```

### PaymentFilterDto
```java
public class PaymentFilterDto {
    @Size(max = 50)
    private String userId;
    
    private List<PaymentStatus> paymentStatuses;
    private List<PaymentMethod> paymentMethods;
    
    @DecimalMin("0.0")
    private BigDecimal minAmount;
    
    @DecimalMin("0.0")
    private BigDecimal maxAmount;
    
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    
    private DateRangePreset dateRangePreset;
    
    @Size(max = 100)
    private String transactionId;
    
    @Builder.Default
    private PaymentSortField sortBy = PaymentSortField.CREATED_AT;
    
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;
    
    @Min(0) @Builder.Default
    private Integer page = 0;
    
    @Min(1) @Builder.Default
    private Integer size = 20;
}
```

## Pagination

### Response Format
All paginated responses use the standardized `PagedResponse<T>` format:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Product Name",
      "basePrice": 299.99
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Pagination Types

#### Offset-Based Pagination
- Standard page/size parameters
- Suitable for most use cases
- Consistent page numbering

```java
// Example usage
filterDto.setPage(0);    // First page (0-based)
filterDto.setSize(20);   // 20 items per page
```

#### Cursor-Based Pagination
- Available for product listings
- Better performance for large datasets
- Prevents duplicate results during concurrent modifications

```java
// Example cursor-based pagination
@GetMapping("/category/{categoryId}/paginated")
public PagedResponse<ProductDto> getByCategoryIdWithPagination(
    @PathVariable Long categoryId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) Long cursor
)
```

### Pagination Limits
- **Default page size**: 20 items
- **Maximum page size**: 100 items
- **Minimum page size**: 1 item
- **Maximum page number**: No limit (validated by total pages)

## Usage Examples

### Basic Product Search
```bash
curl -X POST http://localhost:8080/v1/products/filter/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "gaming",
    "minPrice": 500,
    "maxPrice": 2000,
    "page": 0,
    "size": 10
  }'
```

### Advanced Order Filtering
```bash
curl -X POST http://localhost:8080/v1/orders/filter/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": "user123",
    "orderStatuses": ["CONFIRMED", "SHIPPED"],
    "paymentStatuses": ["PAID"],
    "createdAfter": "2024-01-01T00:00:00",
    "createdBefore": "2024-12-31T23:59:59",
    "sortBy": "CREATED_AT",
    "sortDirection": "DESC"
  }'
```

### Payment Statistics
```bash
curl -X GET "http://localhost:8080/v1/payments/filter/statistics?userId=user123&method=VIETTEL_MONEY" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Dynamic Field Filtering
```bash
curl -X POST http://localhost:8080/v1/products/filter/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "dynamicFields": [
      {
        "fieldName": "brand",
        "value": "Dell",
        "matchType": "EQUALS"
      },
      {
        "fieldName": "processor",
        "value": "Intel",
        "matchType": "CONTAINS"
      }
    ],
    "page": 0,
    "size": 20
  }'
```

## Error Handling

### Validation Errors
```json
{
  "timestamp": "2024-06-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "minPrice",
      "message": "Minimum price must be non-negative"
    },
    {
      "field": "page",
      "message": "Page must be non-negative"
    }
  ]
}
```

### Common Error Codes
- **400 Bad Request**: Invalid filter parameters or validation errors
- **401 Unauthorized**: Missing or invalid authentication token
- **403 Forbidden**: Insufficient permissions for the operation
- **404 Not Found**: Requested resource not found
- **500 Internal Server Error**: Unexpected server error

### Security Error Handling
```json
{
  "timestamp": "2024-06-08T10:30:00Z",
  "status": 400,
  "error": "Security Validation Failed",
  "message": "Input contains potentially malicious content",
  "code": "SECURITY_VIOLATION"
}
```

## Performance Considerations

### Query Optimization
- **Indexed Fields**: All filterable fields are properly indexed
- **Parameterized Queries**: Prevents SQL injection and improves query plan caching
- **Selective Filtering**: Only applies filters when values are provided
- **Lazy Loading**: Related entities loaded only when needed

### Database Indexes
```sql
-- Example indexes for optimal filtering performance
CREATE INDEX idx_product_category_price ON products(category_id, base_price);
CREATE INDEX idx_order_user_status_created ON orders(user_id, status, created_at);
CREATE INDEX idx_payment_status_method_created ON payments(status, payment_method, created_at);
```

### Caching Strategy
- **Query Result Caching**: Frequently accessed filter results cached
- **Statistics Caching**: Aggregated statistics cached with TTL
- **Cache Invalidation**: Automatic cache invalidation on data updates

### Performance Limits
- **Maximum filter criteria**: 10 dynamic fields per query
- **Query timeout**: 30 seconds for complex queries
- **Result set limit**: 10,000 total results per filter operation

## Testing

### Unit Tests
```java
@Test
public void testProductFilterWithSecurityValidation() {
    ProductFilterDto filter = ProductFilterDto.builder()
        .name("'; DROP TABLE products; --")  // SQL injection attempt
        .minPrice(new BigDecimal("100"))
        .build();
    
    // Should be safely handled by parameterized queries
    PagedResponse<ProductDto> result = productFilterService.filterProducts(filter);
    
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty(); // No products match malicious input
}
```

### Integration Tests
```java
@Test
@WithMockUser(roles = "USER")
public void testOrderFilterEndpointSecurity() throws Exception {
    OrderFilterDto filter = new OrderFilterDto();
    filter.setUserId("user123");
    filter.setOrderStatuses(List.of(OrderStatus.CONFIRMED));
    
    mockMvc.perform(post("/v1/orders/filter/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pagination.page").value(0));
}
```

### Security Tests
```java
@Test
public void testSqlInjectionPrevention() {
    PaymentFilterDto filter = PaymentFilterDto.builder()
        .userId("user'; DELETE FROM payments WHERE '1'='1")
        .build();
    
    // Should not cause any database modifications
    assertDoesNotThrow(() -> {
        paymentFilterService.filterPayments(filter);
    });
    
    // Verify database integrity
    long paymentCount = paymentRepository.count();
    assertThat(paymentCount).isGreaterThan(0);
}
```

### Performance Tests
```java
@Test
public void testFilterPerformanceWithLargeDataset() {
    // Create 10,000 test products
    createTestProducts(10000);
    
    ProductFilterDto filter = ProductFilterDto.builder()
        .categoryId(1L)
        .minPrice(new BigDecimal("100"))
        .maxPrice(new BigDecimal("1000"))
        .page(0)
        .size(50)
        .build();
    
    long startTime = System.currentTimeMillis();
    PagedResponse<ProductDto> result = productFilterService.filterProducts(filter);
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    assertThat(result.getContent()).isNotEmpty();
}
```

## Best Practices

### Security
1. **Always validate input**: Use Jakarta Bean Validation annotations
2. **Use parameterized queries**: Never concatenate user input into SQL
3. **Implement rate limiting**: Prevent abuse of filtering endpoints
4. **Sanitize output**: Ensure filtered data doesn't contain sensitive information

### Performance
1. **Index filterable fields**: Create appropriate database indexes
2. **Limit result sets**: Implement reasonable pagination limits
3. **Cache frequently accessed data**: Use appropriate caching strategies
4. **Monitor query performance**: Log slow queries for optimization

### Maintainability
1. **Use consistent naming**: Follow established naming conventions
2. **Document filter criteria**: Clearly document all available filters
3. **Version your APIs**: Plan for future enhancements
4. **Test comprehensively**: Include unit, integration, and security tests

---

## Support and Troubleshooting

For issues or questions regarding the pagination and filtering system:

1. Check the application logs for detailed error messages
2. Verify JWT token validity and permissions
3. Ensure filter parameters meet validation requirements
4. Review database indexes for performance issues
5. Contact the development team for additional support

**Last Updated**: June 8, 2025  
**Version**: 1.0.0  
**Maintained by**: VDT E-Commerce Development Team

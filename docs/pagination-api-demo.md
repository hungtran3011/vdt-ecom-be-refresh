# Product Pagination API Demo

This document demonstrates how to use the newly implemented cursor-based pagination for products by category.

## API Endpoints

### 1. Get Products by Category with Pagination

**Endpoint:** `GET /v1/products/category/{categoryId}/paginated`

**Parameters:**
- `categoryId` (path) - The ID of the category
- `page` (query, optional, default: 0) - Page number (0-based)
- `size` (query, optional, default: 10) - Number of items per page
- `cursor` (query, optional) - Cursor for pagination (ID of last item from previous page)

### 2. Get Previous Page with Cursor

**Endpoint:** `GET /v1/products/category/{categoryId}/paginated/previous`

**Parameters:**
- `categoryId` (path) - The ID of the category
- `page` (query, optional, default: 0) - Page number (0-based)
- `size` (query, optional, default: 10) - Number of items per page
- `cursor` (query, required) - Cursor for going to previous page

## Response Format

```json
{
  "content": [
    {
      "id": 1,
      "name": "Product Name",
      "description": "Product Description",
      "basePrice": 99.99,
      "categoryId": 1,
      "images": ["image1.jpg", "image2.jpg"],
      "dynamicValues": [...],
      "variations": [...]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 50,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false,
    "nextCursor": 10,
    "previousCursor": null
  }
}
```

## Example Usage Scenarios

### Scenario 1: First Page Request

Get the first page of products for category ID 1:

```bash
curl -X GET "http://localhost:8080/v1/products/category/1/paginated?page=0&size=5"
```

**Expected Response:**
```json
{
  "content": [
    // 5 products with IDs 1, 2, 3, 4, 5
  ],
  "pagination": {
    "page": 0,
    "size": 5,
    "totalElements": 23,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false,
    "nextCursor": 5,
    "previousCursor": null
  }
}
```

### Scenario 2: Next Page Using Cursor

Get the next page using the cursor from the previous response:

```bash
curl -X GET "http://localhost:8080/v1/products/category/1/paginated?page=1&size=5&cursor=5"
```

**Expected Response:**
```json
{
  "content": [
    // Next 5 products with IDs 6, 7, 8, 9, 10
  ],
  "pagination": {
    "page": 1,
    "size": 5,
    "totalElements": 23,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": true,
    "nextCursor": 10,
    "previousCursor": 5
  }
}
```

### Scenario 3: Previous Page Using Cursor

Go back to the previous page using the previous endpoint:

```bash
curl -X GET "http://localhost:8080/v1/products/category/1/paginated/previous?page=0&size=5&cursor=10"
```

**Expected Response:**
```json
{
  "content": [
    // Previous 5 products with IDs 5, 4, 3, 2, 1 (reversed and re-ordered)
  ],
  "pagination": {
    "page": 0,
    "size": 5,
    "totalElements": 23,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": true,
    "nextCursor": 10,
    "previousCursor": 1
  }
}
```

### Scenario 4: Large Page Size

Get a larger page size:

```bash
curl -X GET "http://localhost:8080/v1/products/category/1/paginated?page=0&size=20"
```

### Scenario 5: Invalid Category

Request products for a non-existent category:

```bash
curl -X GET "http://localhost:8080/v1/products/category/999/paginated?page=0&size=10"
```

**Expected Response:** HTTP 500 with ProductProcessingException

## Advantages of Cursor-Based Pagination

1. **Performance**: More efficient for large datasets as it doesn't require OFFSET operations
2. **Consistency**: Results remain consistent even when new items are added during pagination
3. **Real-time safe**: Handles concurrent modifications better than offset-based pagination

## Implementation Details

### ProductRepository Methods

- `findByCategory(Category category, Pageable pageable)` - Standard pagination for first page
- `findByCategoryWithCursorAfter(Category category, Long cursor, Pageable pageable)` - Forward pagination
- `findByCategoryWithCursorBefore(Category category, Long cursor, Pageable pageable)` - Backward pagination
- `countByCategory(Category category)` - Get total count for metadata

### Database Queries

**Forward Pagination:**
```sql
SELECT p FROM Product p WHERE p.category = :category AND p.id > :cursor ORDER BY p.id ASC
```

**Backward Pagination:**
```sql
SELECT p FROM Product p WHERE p.category = :category AND p.id < :cursor ORDER BY p.id DESC
```

### Cursor Logic

- **nextCursor**: ID of the last item in current page (used for forward navigation)
- **previousCursor**: ID of the first item in current page (used for backward navigation)
- The cursor is based on the product ID, ensuring stable ordering

## Testing the Implementation

To test the pagination functionality:

1. Ensure you have products in at least one category
2. Use the provided curl commands or Postman
3. Verify the pagination metadata is correct
4. Test edge cases like empty categories or invalid cursors

## Integration with CategoryDto productCount

The pagination works seamlessly with the existing productCount field in CategoryDto:
- The `totalElements` in pagination metadata matches the `productCount` field
- Both use the same `countByCategory` repository method for consistency

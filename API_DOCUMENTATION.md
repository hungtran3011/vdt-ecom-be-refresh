# VDT E-Commerce Backend API Documentation

## Table of Contents

1. [Overview](#overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Common Response Formats](#common-response-formats)
4. [API Endpoints](#api-endpoints)
5. [Data Transfer Objects (DTOs)](#data-transfer-objects-dtos)
6. [API Workflow Examples](#api-workflow-examples)
7. [Error Handling](#error-handling)
8. [Pagination](#pagination)

## Overview

The VDT E-Commerce Backend API is a REST-based service built with Spring Boot, providing comprehensive e-commerce functionality including product management, shopping cart, order processing, user authentication, and payment integration with Viettel Money.

**Base URL:** `http://localhost:8888`

**API Version:** v1

**Content-Type:** `application/json`

## Authentication & Authorization

### Authentication Method

The API uses **JWT Bearer Token** authentication integrated with Keycloak using PKCE (Proof Key for Code Exchange) flow.

### Authorization Header Format

```plaintext
Authorization: Bearer <JWT_TOKEN>
```

### Role-Based Access Control

- **Public Endpoints:** No authentication required
- **User Endpoints:** Require valid JWT token
- **Admin Endpoints:** Require `ROLE_admin` role
- **Seller Endpoints:** Require `ROLE_seller` role

### Available Roles

- `ROLE_admin` - Full administrative access
- `ROLE_seller` - Product and inventory management
- `ROLE_user` - Standard user access (default)

## Common Response Formats

### Success Response

```json
{
  "data": {...},
  "status": "success",
  "message": "Operation completed successfully"
}
```

### Error Response

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": {...}
  },
  "status": "error",
  "timestamp": "2025-06-08T10:30:00Z"
}
```

### Paginated Response

```json
{
  "content": [...],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "hasNext": true,
    "hasPrevious": false,
    "nextCursor": "123",
    "previousCursor": null
  }
}
```

## API Endpoints

### 1. Authentication Endpoints `/v1/auth`

#### Get Current User Information

- **GET** `/v1/auth/me`
- **Auth Required:** Yes
- **Response:**

```json
{
  "userId": "uuid",
  "username": "user@example.com",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ROLE_user"]
}
```

#### Create User (Admin Only)

- **POST** `/v1/auth/admin/users`
- **Auth Required:** Admin role
- **Request Body:** `UserRegistrationDto`
- **Response:** `{"message": "User created successfully", "userId": "uuid"}`

#### Get User by ID (Admin Only)

- **GET** `/v1/auth/admin/users/{userId}`
- **Auth Required:** Admin role
- **Response:** `UserRepresentation`

#### Update User (Admin Only)

- **PUT** `/v1/auth/admin/users/{userId}`
- **Auth Required:** Admin role
- **Request Body:** `UserUpdateDto`

#### Delete User (Admin Only)

- **DELETE** `/v1/auth/admin/users/{userId}`
- **Auth Required:** Admin role

#### Assign Role to User (Admin Only)

- **POST** `/v1/auth/admin/users/roles`
- **Auth Required:** Admin role
- **Request Body:** `RoleAssignmentDto`

#### Remove Role from User (Admin Only)

- **DELETE** `/v1/auth/admin/users/{userId}/roles/{roleName}`
- **Auth Required:** Admin role

#### Get User Roles (Admin Only)

- **GET** `/v1/auth/admin/users/{userId}/roles`
- **Auth Required:** Admin role
- **Response:** `List<RoleRepresentation>`

#### Reset User Password (Admin Only)

- **POST** `/v1/auth/admin/users/reset-password`
- **Auth Required:** Admin role
- **Request Body:** `PasswordResetDto`

#### Enable/Disable User (Admin Only)

- **PUT** `/v1/auth/admin/users/{userId}/enabled`
- **Auth Required:** Admin role
- **Request Body:** `{"enabled": true/false}`

### 2. Category Endpoints `/v1/categories`

#### Get All Categories

- **GET** `/v1/categories`
- **Auth Required:** No
- **Response:** `List<CategoryDto>`

#### Get Category by ID

- **GET** `/v1/categories/{id}`
- **Auth Required:** No
- **Response:** `CategoryDto`

#### Create Category

- **POST** `/v1/categories`
- **Auth Required:** Admin/Seller role
- **Request Body:** `CategoryDto`
- **Response:** `CategoryDto`

#### Update Category

- **PUT** `/v1/categories/{id}`
- **Auth Required:** Admin/Seller role
- **Request Body:** `CategoryDto`
- **Response:** `CategoryDto`

#### Delete Category

- **DELETE** `/v1/categories/{id}`
- **Auth Required:** Admin/Seller role

### 3. Product Endpoints `/v1/products`

#### Get All Products (Paginated)

- **GET** `/v1/products`
- **Auth Required:** No
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (optional, for cursor-based pagination)
- **Response:** `PagedResponse<ProductDto>`

#### Get Product by ID

- **GET** `/v1/products/{id}`
- **Auth Required:** No
- **Response:** `ProductDto`

#### Create Product

- **POST** `/v1/products`
- **Auth Required:** Admin/Seller role
- **Request Body:** `ProductDto`
- **Response:** `ProductDto`

#### Update Product

- **PUT** `/v1/products/{id}`
- **Auth Required:** Admin/Seller role
- **Request Body:** `ProductDto`
- **Response:** `ProductDto`

#### Delete Product

- **DELETE** `/v1/products/{id}`
- **Auth Required:** Admin/Seller role

### 4. Cart Endpoints `/v1/cart`

#### Create Cart

- **POST** `/v1/cart`
- **Auth Required:** Yes
- **Request Body:** `CartDto`
- **Response:** `CartDto`

#### Get Cart by ID

- **GET** `/v1/cart/{id}`
- **Auth Required:** Yes
- **Response:** `CartDto`

#### Update Cart

- **PUT** `/v1/cart/{id}`
- **Auth Required:** Yes
- **Request Body:** `CartDto`
- **Response:** `CartDto`

#### Delete Cart

- **DELETE** `/v1/cart/{id}`
- **Auth Required:** Yes

#### Get Cart Items (Paginated)

- **GET** `/v1/cart/{cartId}/items`
- **Auth Required:** Yes
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (optional)
- **Response:** `PagedResponse<CartItemDto>`

#### Get Previous Page of Cart Items

- **GET** `/v1/cart/{cartId}/items/previous`
- **Auth Required:** Yes
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (required)
- **Response:** `PagedResponse<CartItemDto>`

#### Get User Cart Items (Paginated)

- **GET** `/v1/cart/user/{userId}/items`
- **Auth Required:** Yes
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (optional)
- **Response:** `PagedResponse<CartItemDto>`

#### Get Previous User Cart Items

- **GET** `/v1/cart/user/{userId}/items/previous`
- **Auth Required:** Yes
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (required)
- **Response:** `PagedResponse<CartItemDto>`

### 5. Order Endpoints `/v1/orders`

#### Create Order

- **POST** `/v1/orders`
- **Auth Required:** Yes
- **Request Body:** `OrderDto`
- **Response:** `OrderDto`

#### Get All Orders (Paginated)

- **GET** `/v1/orders`
- **Auth Required:** Admin role
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (optional)
- **Response:** `PagedResponse<OrderDto>`

#### Get Previous Page of Orders

- **GET** `/v1/orders/previous`
- **Auth Required:** Admin role
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
  - `cursor` (required)
- **Response:** `PagedResponse<OrderDto>`

#### Get Order by ID

- **GET** `/v1/orders/{id}`
- **Auth Required:** Yes (own orders) / Admin (all orders)
- **Response:** `OrderDto`

#### Update Order

- **PUT** `/v1/orders/{id}`
- **Auth Required:** Admin role
- **Request Body:** `OrderDto`
- **Response:** `OrderDto`

### 6. Stock Management Endpoints `/v1/stock`

#### Get All Stock Items (Paginated)

- **GET** `/v1/stock`
- **Auth Required:** Admin/Seller role
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 10)
- **Response:** `PagedResponse<StockDto>`

#### Get Stock by ID

- **GET** `/v1/stock/{id}`
- **Auth Required:** Admin/Seller role
- **Response:** `StockDto`

#### Create Stock Item

- **POST** `/v1/stock`
- **Auth Required:** Admin/Seller role
- **Request Body:** `StockDto`
- **Response:** `StockDto`

#### Update Stock Item

- **PUT** `/v1/stock/{id}`
- **Auth Required:** Admin/Seller role
- **Request Body:** `StockDto`
- **Response:** `StockDto`

#### Delete Stock Item

- **DELETE** `/v1/stock/{id}`
- **Auth Required:** Admin/Seller role

### 7. Profile Endpoints `/v1/profiles`

#### Create/Update Profile

- **POST** `/v1/profiles`
- **Auth Required:** Yes
- **Request Body:** `ProfileDto`
- **Response:** `ProfileDto`

#### Get Profile by User ID

- **GET** `/v1/profiles/user/{userId}`
- **Auth Required:** Yes (own profile) / Admin (all profiles)
- **Response:** `ProfileDto`

#### Get All User Profiles (MISSING ENDPOINT)

- **GET** `/v1/profiles` (NOT IMPLEMENTED)
- **Auth Required:** Admin role
- **Query Parameters:**
  - `page` (integer): Page number (0-based)
  - `size` (integer): Page size (default: 20)
  - `sort` (string): Sort field and direction (e.g., "fullName,asc")
- **Response:** `Page<ProfileDto>`
- **Status:** ⚠️ **This endpoint is currently missing from the implementation**
- **Note:** This admin-only endpoint would be needed for user management functionality but is not yet implemented in the ProfileController.

#### Sync Profile from Token

- **POST** `/v1/profiles/me/sync`
- **Auth Required:** Yes
- **Response:** `ProfileDto`

### 8. Address Endpoints `/v1/address`

#### Get Provinces

- **GET** `/v1/address/provinces`
- **Auth Required:** No
- **Response:** `List<Province>`

#### Get Districts by Province

- **GET** `/v1/address/districts/{provinceCode}`
- **Auth Required:** No
- **Response:** `List<District>`

#### Get Wards by District

- **GET** `/v1/address/wards/{districtCode}`
- **Auth Required:** No
- **Response:** `List<Ward>`

### 9. Media Endpoints `/v1/media`

#### Upload Media File

- **POST** `/v1/media/upload`
- **Auth Required:** Yes
- **Request:** Multipart file upload
- **Response:** `{"url": "file_url", "filename": "uploaded_file_name"}`

### 10. Payment Endpoints `/api/payment/viettel`

#### Initiate Payment

- **POST** `/api/payment/viettel/initiate`
- **Auth Required:** Yes
- **Request Body:** `ViettelTransactionInitiationRequest`
- **Response:** `ViettelTransactionInitiationResponse`

#### Query Payment Status

- **POST** `/api/payment/viettel/query`
- **Auth Required:** Yes
- **Request Body:** `ViettelQueryTransactionRequest`
- **Response:** `ViettelQueryTransactionResponse`

#### Process Refund

- **POST** `/api/payment/viettel/refund`
- **Auth Required:** Admin role
- **Request Body:** `ViettelRefundRequest`
- **Response:** `ViettelRefundResponse`

### 11. Viettel Partner Endpoints `/api/viettel/partner`

#### Confirm Order

- **POST** `/api/viettel/partner/order-confirm`
- **Auth Required:** API Key (Viettel Partner)
- **Request Body:** `OrderConfirmationRequest`
- **Response:** `OrderConfirmationResponse`

#### Handle Transaction Result

- **POST** `/api/viettel/partner/transaction-result`
- **Auth Required:** API Key (Viettel Partner)
- **Request Body:** `TransactionResultRequest`
- **Response:** `TransactionResultResponse`

### 12. Statistics Endpoints `/v1/stats`

#### Get System Statistics

- **GET** `/v1/stats/system`
- **Auth Required:** Admin role
- **Response:** `SystemStatsDto`

## Data Transfer Objects (DTOs)

### UserRegistrationDto

```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 6 chars, required)",
  "firstName": "string (required)",
  "lastName": "string (required)",
  "phoneNumber": "string (optional)",
  "roles": ["string"] // optional, defaults to ["ROLE_user"]
}
```

### UserUpdateDto

```json
{
  "email": "string (valid email)",
  "firstName": "string (1-50 chars)",
  "lastName": "string (1-50 chars)"
}
```

### RoleAssignmentDto

```json
{
  "userId": "string (required)",
  "roleName": "string (required)",
  "roles": ["string"] // optional
}
```

### PasswordResetDto

```json
{
  "userId": "string (required)",
  "newPassword": "string (min 6 chars, required)",
  "temporary": "boolean (default: false)"
}
```

### CategoryDto

```json
{
  "id": "number (auto-generated)",
  "name": "string (required)",
  "dynamicFields": [
    {
      "id": "number",
      "name": "string",
      "type": "string", // TEXT, NUMBER, BOOLEAN, etc.
      "required": "boolean",
      "options": ["string"] // for SELECT type
    }
  ],
  "imageUrl": "string",
  "productCount": "number (read-only)"
}
```

### ProductDto

```json
{
  "id": "number (auto-generated)",
  "name": "string (required)",
  "description": "string",
  "basePrice": "number (BigDecimal, required)",
  "images": ["string"], // array of image URLs
  "category": "CategoryDto",
  "categoryId": "number (required)",
  "dynamicValues": [
    {
      "id": "number",
      "field": "CategoryDynamicFieldDto",
      "value": "string"
    }
  ],
  "variations": [
    {
      "id": "number",
      "name": "string",
      "values": ["string"], // e.g., ["Red", "Blue", "Green"]
      "dynamicValues": [
        {
          "variation": "string",
          "field": "CategoryDynamicFieldDto",
          "value": "string"
        }
      ]
    }
  ]
}
```

### CartDto

```json
{
  "id": "number (auto-generated)",
  "userId": "number",
  "items": [
    {
      "id": "number",
      "productId": "number",
      "productName": "string",
      "selectedVariations": ["VariationDto"],
      "quantity": "number",
      "unitPrice": "number (BigDecimal)",
      "subtotal": "number (BigDecimal)",
      "stockSku": "string",
      "addedAt": "datetime"
    }
  ],
  "totalPrice": "number (BigDecimal)",
  "lastUpdated": "datetime"
}
```

### CartItemDto

```json
{
  "id": "number (auto-generated)",
  "productId": "number (required)",
  "productName": "string",
  "selectedVariations": [
    {
      "id": "number",
      "name": "string",
      "values": ["string"]
    }
  ],
  "quantity": "number (required, min: 1)",
  "unitPrice": "number (BigDecimal)",
  "subtotal": "number (BigDecimal, calculated)",
  "stockSku": "string",
  "addedAt": "datetime"
}
```

### OrderDto

```json
{
  "id": "string (UUID, auto-generated)",
  "userId": "string (UUID, required)",
  "status": "string", // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
  "address": "string (required)",
  "phone": "string (required)",
  "note": "string",
  "paymentMethod": "string", // VIETTEL_MONEY, BANK_TRANSFER, COD
  "paymentStatus": "string", // PENDING, COMPLETED, FAILED, REFUNDED
  "paymentId": "string", // Payment gateway transaction ID
  "totalPrice": "number (BigDecimal, required)",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "items": [
    {
      "id": "number",
      "orderId": "string",
      "productId": "number",
      "productName": "string",
      "productImage": "string",
      "quantity": "number",
      "price": "number (BigDecimal)",
      "totalPrice": "number (BigDecimal)"
    }
  ]
}
```

### StockDto

```json
{
  "id": "number (auto-generated)",
  "sku": "string (unique identifier)",
  "productId": "number (required)",
  "productName": "string",
  "variations": [
    {
      "id": "number",
      "name": "string",
      "values": ["string"]
    }
  ],
  "quantity": "number (required, min: 0)",
  "lowStockThreshold": "number (default: 10)",
  "status": "string", // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
  "updatedAt": "datetime"
}
```

### ProfileDto

```json
{
  "userId": "string (UUID, required)",
  "fullName": "string",
  "phone": "string",
  "email": "string",
  "dateOfBirth": "date",
  "address": {
    "provinceCode": "number",
    "districtCode": "number",
    "wardCode": "number",
    "detailed": "string" // house number and street
  }
}
```

### ViettelTransactionInitiationRequest

```json
{
  "orderId": "string (required)",
  "transAmount": "number (required, amount in VND)",
  "description": "string",
  "returnType": "string", // WEB, QR, DEEPLINK
  "returnUrl": "string", // for WEB returnType
  "cancelUrl": "string", // for WEB returnType
  "paymentMethod": "string",
  "expireAfter": "number", // minutes
  "customerInfo": {
    "customerName": "string",
    "customerPhone": "string",
    "customerEmail": "string",
    "customerAddress": "string"
  }
}
```

### SystemStatsDto

```json
{
  "totalUsers": "number",
  "totalProducts": "number",
  "totalOrders": "number",
  "totalRevenue": "number (BigDecimal)",
  "totalStockValue": "number (BigDecimal)",
  "lowStockProducts": "number",
  "pendingOrders": "number",
  "completedOrders": "number"
}
```

## API Workflow Examples

### 1. Category Management Workflow

#### A. Create Category

```bash
# 1. Create a new category
POST /v1/categories
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "name": "Electronics",
  "imageUrl": "https://example.com/electronics.jpg",
  "dynamicFields": [
    {
      "name": "Brand",
      "type": "TEXT",
      "required": true
    },
    {
      "name": "Warranty",
      "type": "NUMBER",
      "required": false
    }
  ]
}
```

#### B. Update Category

```bash
# 2. Update the category
PUT /v1/categories/1
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "id": 1,
  "name": "Electronics & Gadgets",
  "imageUrl": "https://example.com/electronics-updated.jpg",
  "dynamicFields": [
    {
      "id": 1,
      "name": "Brand",
      "type": "TEXT",
      "required": true
    },
    {
      "id": 2,
      "name": "Warranty",
      "type": "NUMBER",
      "required": false
    },
    {
      "name": "Color",
      "type": "SELECT",
      "required": false,
      "options": ["Red", "Blue", "Black", "White"]
    }
  ]
}
```

#### C. Delete Category

```bash
# 3. Delete the category (ensure no products are assigned)
DELETE /v1/categories/1
Authorization: Bearer <admin_jwt_token>
```

### 2. Product Management Workflow

#### A. Create Product with Variations

```bash
# 1. Create a product with variations
POST /v1/products
Authorization: Bearer <seller_jwt_token>
Content-Type: application/json

{
  "name": "Smartphone XYZ",
  "description": "Latest smartphone with advanced features",
  "basePrice": 599.99,
  "images": [
    "https://example.com/phone1.jpg",
    "https://example.com/phone2.jpg"
  ],
  "categoryId": 1,
  "dynamicValues": [
    {
      "field": {
        "id": 1,
        "name": "Brand"
      },
      "value": "TechCorp"
    },
    {
      "field": {
        "id": 2,
        "name": "Warranty"
      },
      "value": "24"
    }
  ],
  "variations": [
    {
      "name": "Color",
      "values": ["Black", "White", "Blue"]
    },
    {
      "name": "Storage",
      "values": ["128GB", "256GB", "512GB"]
    }
  ]
}
```

#### B. Update Product

```bash
# 2. Update product details
PUT /v1/products/1
Authorization: Bearer <seller_jwt_token>
Content-Type: application/json

{
  "id": 1,
  "name": "Smartphone XYZ Pro",
  "description": "Updated description with new features",
  "basePrice": 649.99,
  "images": [
    "https://example.com/phone1-updated.jpg",
    "https://example.com/phone2-updated.jpg",
    "https://example.com/phone3-new.jpg"
  ],
  "categoryId": 1,
  "dynamicValues": [
    {
      "field": {
        "id": 1,
        "name": "Brand"
      },
      "value": "TechCorp"
    },
    {
      "field": {
        "id": 2,
        "name": "Warranty"
      },
      "value": "36"
    }
  ]
}
```

#### C. Stock Management for Product Variations

```bash
# 3. Create stock entries for product variations
POST /v1/stock
Authorization: Bearer <seller_jwt_token>
Content-Type: application/json

{
  "sku": "PHONE-XYZ-BLACK-128GB",
  "productId": 1,
  "variations": [
    {
      "name": "Color",
      "values": ["Black"]
    },
    {
      "name": "Storage",
      "values": ["128GB"]
    }
  ],
  "quantity": 50,
  "lowStockThreshold": 10
}
```

#### D. Delete Product

```bash
# 4. Delete product (will also remove associated stock)
DELETE /v1/products/1
Authorization: Bearer <seller_jwt_token>
```

### 3. Payment Processing Workflow

#### A. Initiate Viettel Money Payment

```bash
# 1. Initiate payment for an order
POST /api/payment/viettel/initiate
Authorization: Bearer <user_jwt_token>
Content-Type: application/json

{
  "orderId": "ORDER-12345",
  "transAmount": 59999, // Amount in VND (599.99 * 100)
  "description": "Payment for order ORDER-12345",
  "returnType": "WEB",
  "returnUrl": "http://localhost:3000/payment/success",
  "cancelUrl": "http://localhost:3000/payment/cancel",
  "expireAfter": 30,
  "customerInfo": {
    "customerName": "John Doe",
    "customerPhone": "0987654321",
    "customerEmail": "john.doe@example.com",
    "customerAddress": "123 Main St, Ho Chi Minh City"
  }
}
```

#### B. Query Payment Status

```bash
# 2. Check payment status
POST /api/payment/viettel/query
Authorization: Bearer <user_jwt_token>
Content-Type: application/json

{
  "orderId": "ORDER-12345"
}
```

#### C. Process Refund (Admin)

```bash
# 3. Process refund for a payment
POST /api/payment/viettel/refund
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "originalRequestId": "VT-REQ-12345",
  "refundAmount": 59999,
  "refundReason": "Customer request"
}
```

### 4. User Profile Management Workflow (Admin Operations)

#### A. Create User

```bash
# 1. Admin creates a new user
POST /v1/auth/admin/users
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "username": "newuser@example.com",
  "email": "newuser@example.com",
  "password": "SecurePass123",
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "0987654321",
  "roles": ["ROLE_seller"]
}
```

#### B. Update User Information

```bash
# 2. Update user details
PUT /v1/auth/admin/users/USER-UUID-12345
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "email": "jane.smith.updated@example.com",
  "firstName": "Jane Elizabeth",
  "lastName": "Smith"
}
```

#### C. Assign Additional Role

```bash
# 3. Assign admin role to user
POST /v1/auth/admin/users/roles
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "userId": "USER-UUID-12345",
  "roleName": "ROLE_admin"
}
```

#### D. Reset User Password

```bash
# 4. Reset user password
POST /v1/auth/admin/users/reset-password
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "userId": "USER-UUID-12345",
  "newPassword": "NewSecurePass456",
  "temporary": true
}
```

#### E. Disable User Account

```bash
# 5. Disable user account
PUT /v1/auth/admin/users/USER-UUID-12345/enabled
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json

{
  "enabled": false
}
```

#### F. Remove Role from User

```bash
# 6. Remove seller role from user
DELETE /v1/auth/admin/users/USER-UUID-12345/roles/ROLE_seller
Authorization: Bearer <admin_jwt_token>
```

#### G. Get User Roles

```bash
# 7. Check user's current roles
GET /v1/auth/admin/users/USER-UUID-12345/roles
Authorization: Bearer <admin_jwt_token>
```

## Error Handling

### Common HTTP Status Codes

- **200 OK** - Request successful
- **201 Created** - Resource created successfully
- **400 Bad Request** - Invalid request data
- **401 Unauthorized** - Authentication required
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **409 Conflict** - Resource conflict (e.g., duplicate)
- **422 Unprocessable Entity** - Validation errors
- **500 Internal Server Error** - Server error

### Error Response Examples

#### Validation Error

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "username": "Username is required",
      "email": "Email should be valid"
    }
  },
  "status": "error",
  "timestamp": "2025-06-08T10:30:00Z"
}
```

#### Authentication Error

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired JWT token"
  },
  "status": "error",
  "timestamp": "2025-06-08T10:30:00Z"
}
```

#### Resource Not Found

```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product not found with id: 123"
  },
  "status": "error",
  "timestamp": "2025-06-08T10:30:00Z"
}
```

## Pagination

The API supports both **offset-based** and **cursor-based** pagination for improved performance with large datasets.

### Offset-Based Pagination

- Use `page` and `size` parameters
- Good for small to medium datasets
- Supports jumping to specific pages

### Cursor-Based Pagination

- Use `cursor` parameter with `page` and `size`
- Better performance for large datasets
- Provides consistent results during concurrent modifications
- Use `nextCursor` and `previousCursor` from response for navigation

### Pagination Example

```bash
# First page
GET /v1/products?page=0&size=20

# Next page using cursor
GET /v1/products?page=1&size=20&cursor=123

# Previous page using cursor
GET /v1/products/previous?page=0&size=20&cursor=123
```

### Pagination Response Structure

```json
{
  "content": [...],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": true,
    "nextCursor": "145",
    "previousCursor": "105"
  }
}
```

## API Completeness Assessment

### ✅ Fully Implemented Features

1. **Authentication & Authorization** - Complete JWT-based auth with Keycloak
2. **User Management** - Full CRUD operations with role management
3. **Product Management** - Complete product lifecycle with variations
4. **Category Management** - Dynamic fields and product association
5. **Cart Operations** - Full shopping cart functionality
6. **Order Processing** - Order creation, tracking, and management
7. **Stock Management** - Inventory tracking with variation support
8. **Payment Integration** - Viettel Money payment gateway
9. **Profile Management** - User profile and address management
10. **Media Upload** - File upload and management
11. **Statistics** - System statistics and reporting
12. **Pagination** - Advanced cursor-based pagination

### ⚠️ Areas for Enhancement

1. **Search & Filtering** - Could benefit from advanced search capabilities
2. **Notifications** - Email notifications are implemented but could be expanded
3. **Audit Logging** - Consider adding comprehensive audit trails
4. **Rate Limiting** - API rate limiting not explicitly implemented
5. **Webhooks** - Webhook support for external integrations

### 🔧 Recommendations for Front-end Integration

1. **JWT Management** - Implement proper token refresh mechanism
2. **Error Handling** - Create comprehensive error handling for all scenarios
3. **Loading States** - Use pagination metadata for loading indicators
4. **Real-time Updates** - Consider WebSocket integration for real-time features
5. **Offline Support** - Implement offline capabilities for cart and favorites

This API provides a solid foundation for building a comprehensive e-commerce front-end application with all essential features properly implemented and documented.

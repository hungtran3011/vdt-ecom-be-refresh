# VDT E-commerce Database ERD

```mermaid
erDiagram
    %% User Profile Management
    profiles ||--o{ orders : "places"
    profiles {
        bigint id PK
        uuid user_id UK "Keycloak user ID"
        varchar full_name
        varchar phone
        varchar email
        date date_of_birth
        integer province_code
        varchar province_name
        integer district_code
        varchar district_name
        integer ward_code
        varchar ward_name
        varchar detailed "Address details"
    }

    %% Cart System
    cart ||--o{ cart_item : "contains"
    cart {
        bigint id PK
        varchar session_id
        bigint user_id
        timestamp created_at
        timestamp last_updated
    }

    cart_item }o--|| product : "references"
    cart_item ||--o{ cart_item_variation : "has_variations"
    cart_item {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        varchar stock_sku
        integer quantity
        decimal unit_price
        timestamp added_at
    }

    %% Product Catalog System
    categories ||--o{ products : "categorizes"
    categories ||--o{ category_dynamic_fields : "defines_fields"
    categories {
        bigint id PK
        varchar name UK
        varchar image_url
    }

    category_dynamic_fields {
        bigint id PK
        bigint category_id FK
        varchar field_name
        enum field_type "TEXT, NUMBER, BOOLEAN, etc"
        enum applies_to "PRODUCT, VARIATION"
        boolean required
    }

    products ||--o{ product_dynamic_values : "has_attributes"
    products ||--o{ variations : "has_variations"
    products ||--o{ stock : "has_stock"
    products {
        bigint id PK
        bigint category_id FK
        varchar name
        text description
        decimal base_price
        jsonb images "Array of image URLs"
    }

    product_dynamic_values }o--|| category_dynamic_fields : "uses_field"
    product_dynamic_values {
        bigint id PK
        bigint product_id FK
        bigint field_id FK
        varchar value
    }

    variations ||--o{ variation_dynamic_values : "has_attributes"
    variations ||--o{ stock_variation : "stock_combinations"
    variations ||--o{ cart_item_variation : "cart_selections"
    variations {
        bigint id PK
        bigint product_id FK
        varchar type "color, RAM, storage, etc"
        varchar name "Blue, 16GB, etc"
        decimal additional_price
    }

    variation_dynamic_values }o--|| category_dynamic_fields : "uses_field"
    variation_dynamic_values {
        bigint id PK
        bigint variation_id FK
        bigint field_id FK
        varchar value
    }

    %% Stock Management
    stock ||--o{ stock_history : "tracks_changes"
    stock ||--o{ stock_variation : "variation_combinations"
    stock {
        bigint id PK
        varchar sku UK
        bigint product_id FK
        integer quantity
        integer low_stock_threshold
        enum status "IN_STOCK, OUT_OF_STOCK, LOW_STOCK, PRE_ORDER"
        timestamp updated_at
        date expected_restock_date
        integer max_pre_order_quantity
        integer pre_order_count
    }

    stock_history {
        bigint id PK
        bigint stock_id FK
        integer quantity_before
        integer quantity_after
        enum action "INBOUND, OUTBOUND, ADJUSTMENT, etc"
        varchar reference
        timestamp timestamp
        varchar updated_by
    }

    %% Junction Tables
    stock_variation {
        bigint stock_id FK
        bigint variation_id FK
    }

    cart_item_variation {
        bigint cart_item_id FK
        bigint variation_id FK
    }

    %% Order Management
    orders ||--o{ order_items : "contains"
    orders ||--o{ payment_history : "payment_records"
    orders {
        varchar id PK "UUID"
        varchar user_id "Keycloak user ID"
        enum status "PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED"
        varchar address
        varchar phone
        text note
        enum payment_method "CASH_ON_DELIVERY, BANK_TRANSFER, VIETTEL_MONEY"
        enum payment_status "PENDING, COMPLETED, FAILED, REFUNDED"
        varchar payment_id "Gateway transaction ID"
        decimal total_price
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        bigint id PK
        varchar order_id FK
        bigint product_id
        varchar product_name
        varchar product_image
        integer quantity
        decimal price
        decimal total_price
    }

    %% Payment System
    payment_history {
        bigint id PK
        varchar order_id FK
        varchar user_id
        varchar gateway_transaction_id
        enum payment_method "CASH_ON_DELIVERY, BANK_TRANSFER, VIETTEL_MONEY"
        enum status "PENDING, COMPLETED, FAILED, REFUNDED"
        decimal amount
        varchar currency "VND"
        timestamp payment_date
        text gateway_response "Raw gateway response"
        varchar error_code
        varchar error_message
        decimal refund_amount
        timestamp refund_date
        varchar refund_transaction_id
        timestamp created_at
        timestamp updated_at
    }
```

## Key Relationships and Design Notes:

### 1. **Dynamic Product Attributes System**
- Uses `category_dynamic_fields` to define flexible attributes for different product categories
- `product_dynamic_values` and `variation_dynamic_values` store actual values
- Supports both product-level and variation-level attributes

### 2. **Flexible Stock Management**
- Stock entries can be associated with specific product-variation combinations
- Comprehensive stock history tracking for auditing
- Support for pre-orders and low stock alerts

### 3. **User Management Integration**
- Uses Keycloak for authentication (user_id references external system)
- Local profile storage for additional user information

### 4. **Cart and Order Flow**
- Guest carts (session-based) and user carts supported
- Complex variation selection in cart items
- Order items store snapshot of product data at time of purchase

### 5. **Payment Integration**
- Designed for Viettel Money payment gateway integration
- Comprehensive payment history tracking
- Support for refunds and payment status tracking

### 6. **Indexing Strategy**
- Strategic database indexes on frequently queried columns
- Performance optimized for e-commerce workloads

This ERD represents a comprehensive e-commerce system with flexible product catalogs, sophisticated stock management, and integrated payment processing specifically designed for Vietnamese market requirements.

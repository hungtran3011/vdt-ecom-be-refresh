# VDT E-commerce UML Package Diagram

```mermaid
graph TB
    %% Main Application Layer
    subgraph "VDT E-commerce Application"
        APP[VdtEcomBeRefreshApplication]
    end

    %% Core Infrastructure Packages
    subgraph "Core Infrastructure"
        CONFIG[config]
        COMMON[common]
        EXCEPTION[exception]
        UTIL[util]
        
        subgraph "config"
            C1[SecurityConfig]
            C2[KeycloakConfig]
            C3[RedisConfig]
            C4[CloudinaryConfig]
            C5[GlobalExceptionHandler]
        end
        
        subgraph "common"
            COM1[enums]
            COM2[dtos]
            COM3[converters]
        end
        
        subgraph "exception"
            EX1[payment]
            EX2[EntityNotFoundException]
            EX3[ValidationException]
        end
    end

    %% Authentication & Authorization Domain
    subgraph "Authentication Domain"
        AUTH[auth]
        PROFILE[profile]
        
        subgraph "auth"
            A1[controllers]
            A2[services]
            A3[dto]
            A4[AuthController]
            A5[AuthService]
        end
        
        subgraph "profile"
            P1[entities/Profile]
            P2[entities/Address]
            P3[controllers]
            P4[services]
            P5[dtos]
            P6[mappers]
            P7[repository]
        end
    end

    %% Product Catalog Domain
    subgraph "Product Catalog Domain"
        PRODUCT[product]
        CATEGORY[category]
        MEDIA[media]
        SEARCH[search]
        
        subgraph "product"
            PR1[entities/Product]
            PR2[entities/Variation]
            PR3[entities/ProductDynamicValue]
            PR4[entities/VariationDynamicValue]
            PR5[controllers]
            PR6[services]
            PR7[dtos]
            PR8[mappers]
            PR9[repositories]
            PR10[validators]
        end
        
        subgraph "category"
            CAT1[entities/Category]
            CAT2[entities/CategoryDynamicField]
            CAT3[controllers]
            CAT4[services]
            CAT5[dtos]
            CAT6[mappers]
            CAT7[repositories]
        end
        
        subgraph "media"
            M1[controllers]
            M2[services]
            M3[entities]
        end
        
        subgraph "search"
            S1[controllers]
            S2[services]
            S3[elasticsearch]
        end
    end

    %% Inventory Management Domain
    subgraph "Inventory Domain"
        STOCK[stock]
        
        subgraph "stock"
            ST1[entities/Stock]
            ST2[entities/StockHistory]
            ST3[controllers]
            ST4[services]
            ST5[dtos]
            ST6[mappers]
            ST7[repositories]
            ST8[enums/StockStatus]
            ST9[enums/StockActionState]
        end
    end

    %% Order Processing Domain
    subgraph "Order Processing Domain"
        CART[cart]
        ORDER[order]
        
        subgraph "cart"
            CR1[entities/Cart]
            CR2[entities/CartItem]
            CR3[controllers]
            CR4[services]
            CR5[dtos]
            CR6[mappers]
            CR7[repositories]
        end
        
        subgraph "order"
            OR1[entities/Order]
            OR2[entities/OrderItem]
            OR3[controllers]
            OR4[services]
            OR5[dtos]
            OR6[mappers]
            OR7[repositories]
            OR8[enums/OrderStatus]
            OR9[enums/PaymentMethod]
        end
    end

    %% Payment Domain
    subgraph "Payment Domain"
        PAYMENT[payment]
        
        subgraph "payment"
            PAY1[config/ViettelPaymentConfig]
            PAY2[controllers/ViettelPaymentController]
            PAY3[controllers/ViettelPartnerController]
            PAY4[services/ViettelPaymentService]
            PAY5[services/ViettelApiClient]
            PAY6[security/ViettelSignatureHandler]
            PAY7[entities/PaymentHistory]
            PAY8[dtos/viettel]
            PAY9[dtos/partner]
            PAY10[mappers]
            PAY11[repositories]
            PAY12[utils]
        end
    end

    %% Communication & Notification Domain
    subgraph "Communication Domain"
        MAIL[mail]
        
        subgraph "mail"
            ML1[services/NotificationService]
            ML2[controllers]
            ML3[templates]
            ML4[email-providers]
        end
    end

    %% Integration Domain
    subgraph "Integration Domain"
        INTEGRATION[integration]
        STATS[stats]
        
        subgraph "integration"
            INT1[external-apis]
            INT2[webhooks]
            INT3[third-party-services]
        end
        
        subgraph "stats"
            STAT1[analytics]
            STAT2[reporting]
            STAT3[metrics]
        end
    end

    %% Dependencies and Relationships
    AUTH --> CONFIG
    AUTH --> COMMON
    PROFILE --> AUTH
    PROFILE --> COMMON
    
    PRODUCT --> CATEGORY
    PRODUCT --> MEDIA
    PRODUCT --> COMMON
    CATEGORY --> COMMON
    SEARCH --> PRODUCT
    SEARCH --> CATEGORY
    
    STOCK --> PRODUCT
    STOCK --> COMMON
    
    CART --> PRODUCT
    CART --> STOCK
    CART --> PROFILE
    CART --> COMMON
    
    ORDER --> CART
    ORDER --> PROFILE
    ORDER --> PAYMENT
    ORDER --> MAIL
    ORDER --> COMMON
    
    PAYMENT --> ORDER
    PAYMENT --> CONFIG
    PAYMENT --> COMMON
    PAYMENT --> MAIL
    
    MAIL --> ORDER
    MAIL --> PAYMENT
    MAIL --> PROFILE
    MAIL --> CONFIG
    
    INTEGRATION --> PAYMENT
    INTEGRATION --> ORDER
    INTEGRATION --> CONFIG
    
    STATS --> ORDER
    STATS --> PAYMENT
    STATS --> PRODUCT
    
    %% External Dependencies
    APP --> AUTH
    APP --> CONFIG
    APP --> COMMON

    %% Styling
    classDef domainBox fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef coreBox fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef serviceBox fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef controllerBox fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef entityBox fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    
    class AUTH,PROFILE domainBox
    class PRODUCT,CATEGORY,MEDIA,SEARCH domainBox
    class STOCK domainBox
    class CART,ORDER domainBox
    class PAYMENT domainBox
    class MAIL domainBox
    class INTEGRATION,STATS domainBox
    class CONFIG,COMMON,EXCEPTION,UTIL coreBox
```

## Package Architecture Overview

### **Core Infrastructure Layer**
- **config**: Cross-cutting configuration classes (Security, Keycloak, Redis, Cloudinary)
- **common**: Shared DTOs, enums, converters used across all domains
- **exception**: Global exception handling and domain-specific exceptions
- **util**: Utility classes and helper functions

### **Domain-Driven Architecture**

#### **1. Authentication Domain**
- **auth**: User authentication, JWT token management, Keycloak integration
- **profile**: User profile management with embedded address information

#### **2. Product Catalog Domain**
- **product**: Core product entities with dynamic attributes and variations
- **category**: Hierarchical product categorization with dynamic field definitions
- **media**: Product image and media asset management
- **search**: Elasticsearch integration for product search and filtering

#### **3. Inventory Domain**
- **stock**: Stock level management with comprehensive history tracking
- Supports pre-orders, low stock alerts, and multi-variation inventory

#### **4. Order Processing Domain**
- **cart**: Shopping cart management with session and user support
- **order**: Complete order lifecycle management with state machine pattern

#### **5. Payment Domain**
- **payment**: Comprehensive Viettel Money payment gateway integration
- Includes API clients, signature handling, webhook processing, and configuration management

#### **6. Communication Domain**
- **mail**: Email notification system with template support
- Handles order confirmations, payment notifications, and system alerts

#### **7. Integration Domain**
- **integration**: External service integrations and webhooks
- **stats**: Analytics, reporting, and metrics collection

### **Key Architectural Patterns**

#### **Layered Architecture**
Each domain follows a consistent layered structure:
- **Controllers**: REST API endpoints
- **Services**: Business logic implementation
- **Repositories**: Data access layer
- **Entities**: JPA domain models
- **DTOs**: Data transfer objects
- **Mappers**: Entity-DTO conversion (MapStruct)

#### **Domain-Driven Design**
- Clear domain boundaries with minimal cross-domain dependencies
- Each domain encapsulates related business logic
- Shared kernel approach for common utilities and configurations

#### **Dependency Flow**
- **Core Infrastructure** ← All Domains
- **Product Catalog** ← **Inventory**, **Order Processing**
- **Authentication** ← **Order Processing**, **Communication**
- **Payment** ↔ **Order Processing** (bidirectional)
- **Communication** ← Multiple domains for notifications

#### **Viettel Payment Integration Architecture**
The payment domain demonstrates sophisticated integration patterns:
- **Config Pattern**: Environment-specific configuration management
- **Gateway Pattern**: ViettelApiClient for external API abstraction
- **Facade Pattern**: ViettelPaymentService for simplified payment operations
- **Security Handler**: Dedicated signature verification and generation
- **Dual Controllers**: Separate controllers for frontend and partner callbacks

This architecture supports the Vietnamese e-commerce market requirements with robust payment processing, flexible product catalogs, and comprehensive order management while maintaining clean separation of concerns and scalability.

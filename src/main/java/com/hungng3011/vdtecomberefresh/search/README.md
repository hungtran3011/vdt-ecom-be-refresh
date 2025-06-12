# Search Module

## Overview

The search module provides comprehensive full-text search capabilities for the VDT E-commerce platform using Elasticsearch. It includes advanced search features, faceted filtering, autocomplete suggestions, and search analytics.

## Features

- **Full-text Search**: Multi-field search across product name, description, SKU, brand, and tags
- **Search Types**: Exact match, fuzzy search, wildcard, phrase prefix, and multi-match
- **Faceted Search**: Category, brand, price range, rating, and availability filters
- **Autocomplete**: Smart suggestions for search queries
- **Search Analytics**: Track search behavior, popular queries, and failed searches
- **Product Indexing**: Automatic synchronization between Product entities and Elasticsearch

## Architecture

```
search/
├── config/           # Elasticsearch configuration
├── controllers/      # REST API endpoints
├── documents/        # Elasticsearch document models
├── dtos/            # Data transfer objects
├── repositories/    # Elasticsearch repositories
└── services/        # Business logic
```

## Components

### Documents
- `ProductSearchDocument`: Elasticsearch document representing a searchable product

### DTOs
- `SearchRequestDto`: Comprehensive search request with filters and options
- `SearchResponseDto`: Search response with results, metadata, facets, and suggestions

### Services
- `ProductSearchService`: Main search operations and query processing
- `ProductIndexingService`: Product synchronization and index management
- `SearchAnalyticsService`: Search behavior tracking and analytics

### Controllers
- `ProductSearchController`: REST API endpoints for search operations

## Configuration

### Elasticsearch Settings (application.yml)
```yaml
elasticsearch:
  host: ${ELASTICSEARCH_HOST:localhost}
  port: ${ELASTICSEARCH_PORT:9200}
  username: ${ELASTICSEARCH_USERNAME:}
  password: ${ELASTICSEARCH_PASSWORD:}
  use-ssl: ${ELASTICSEARCH_USE_SSL:false}
```

### Docker Compose
Elasticsearch is included in the compose.yaml with proper configuration for development.

## API Endpoints

### Search Products
- `POST /v1/search/products` - Advanced search with full filtering
- `GET /v1/search/products` - Quick search with query parameters

### Suggestions & Facets
- `GET /v1/search/suggestions` - Autocomplete suggestions
- `POST /v1/search/facets` - Get faceted search results

### Analytics
- `GET /v1/search/popular` - Popular search queries
- `POST /v1/search/click` - Log search result clicks
- `GET /v1/search/analytics/dashboard` - Admin analytics dashboard

## Usage Examples

### Basic Search
```bash
curl -X GET "localhost:8888/api/v1/search/products?q=laptop&page=0&size=20"
```

### Advanced Search
```bash
curl -X POST "localhost:8888/api/v1/search/products" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "gaming laptop",
    "searchType": "FUZZY",
    "filters": {
      "categoryIds": [1, 2],
      "minPrice": 500,
      "maxPrice": 2000,
      "brands": ["Dell", "HP"],
      "inStock": true
    },
    "page": 0,
    "size": 20,
    "sortBy": "price",
    "sortDirection": "ASC",
    "includeFacets": true,
    "includeSuggestions": true
  }'
```

### Get Suggestions
```bash
curl -X GET "localhost:8888/api/v1/search/suggestions?q=lap&limit=10"
```

## Development

### Running with Docker
```bash
# Start all services including Elasticsearch
docker-compose up -d

# Check Elasticsearch health
curl http://localhost:9200/_cluster/health
```

### Testing Search
```bash
# Health check
curl http://localhost:8888/api/v1/search/health

# Test search
curl "localhost:8888/api/v1/search/products?q=test"
```

## Index Management

### Manual Reindexing
The system provides automatic indexing, but manual operations are available:

- Full reindex runs daily at 2 AM
- Incremental reindex runs every 15 minutes
- Manual reindex by category available via service methods

### Monitoring
- Search performance metrics
- Index size and document count
- Failed search queries for content optimization
- Popular search terms for trending analysis

## Performance Considerations

- Uses simplified repository-based queries for better compatibility
- Faceted search is optimized for common use cases
- Autocomplete suggestions are cached for performance
- Search analytics are logged asynchronously

## Future Enhancements

- Advanced Elasticsearch query DSL integration
- Machine learning for search result ranking
- Personalized search based on user behavior
- Multi-language search support
- Synonym management
- A/B testing for search algorithms

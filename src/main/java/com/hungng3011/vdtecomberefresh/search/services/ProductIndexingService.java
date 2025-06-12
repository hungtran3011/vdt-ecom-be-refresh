package com.hungng3011.vdtecomberefresh.search.services;

import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import com.hungng3011.vdtecomberefresh.search.repositories.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for indexing products into Elasticsearch for search
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexingService {
    
    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    
    /**
     * Index a single product
     */
    @Async
    public void indexProduct(Product product) {
        try {
            ProductSearchDocument document = convertToSearchDocument(product);
            productSearchRepository.save(document);
            
            log.debug("Indexed product: {} (ID: {})", product.getName(), product.getId());
        } catch (Exception e) {
            log.error("Failed to index product: {} (ID: {})", product.getName(), product.getId(), e);
        }
    }
    
    /**
     * Index multiple products in batch
     */
    @Async
    public void indexProducts(List<Product> products) {
        try {
            List<ProductSearchDocument> documents = products.stream()
                .map(this::convertToSearchDocument)
                .collect(Collectors.toList());
                
            productSearchRepository.saveAll(documents);
            
            log.info("Batch indexed {} products", products.size());
        } catch (Exception e) {
            log.error("Failed to batch index {} products", products.size(), e);
        }
    }
    
    /**
     * Delete a product from search index
     */
    @Async
    public void deleteProductFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(productId.toString());
            log.debug("Deleted product from search index: {}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product from search index: {}", productId, e);
        }
    }
    
    /**
     * Full reindex of all products - scheduled to run daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void fullReindex() {
        log.info("Starting full product reindex");
        
        try {
            // Clear existing index
            productSearchRepository.deleteAll();
            
            int batchSize = 1000;
            int page = 0;
            long totalProcessed = 0;
            
            Page<Product> productPage;
            do {
                productPage = productRepository.findAll(Pageable.ofSize(batchSize).withPage(page));
                
                if (!productPage.isEmpty()) {
                    indexProducts(productPage.getContent());
                    totalProcessed += productPage.getNumberOfElements();
                    
                    log.info("Processed {} products (Total: {})", 
                        productPage.getNumberOfElements(), totalProcessed);
                }
                
                page++;
                
            } while (productPage.hasNext());
            
            log.info("Full reindex completed. Total products indexed: {}", totalProcessed);
            
        } catch (Exception e) {
            log.error("Full reindex failed", e);
        }
    }
    
    /**
     * Incremental reindex of recently updated products - runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional(readOnly = true)
    public void incrementalReindex() {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(16); // Slight overlap to avoid missing updates
            
            List<Product> recentlyUpdated = productRepository.findByUpdatedAtAfter(since);
            
            if (!recentlyUpdated.isEmpty()) {
                indexProducts(recentlyUpdated);
                log.info("Incremental reindex completed. {} products updated", recentlyUpdated.size());
            }
            
        } catch (Exception e) {
            log.error("Incremental reindex failed", e);
        }
    }
    
    /**
     * Reindex products by category
     */
    @Async
    public void reindexByCategory(Long categoryId) {
        try {
            List<Product> products = productRepository.findByCategoryId(categoryId);
            
            if (!products.isEmpty()) {
                indexProducts(products);
                log.info("Reindexed {} products for category {}", products.size(), categoryId);
            }
            
        } catch (Exception e) {
            log.error("Failed to reindex products for category {}", categoryId, e);
        }
    }
    
    /**
     * Update product availability in search index
     */
    @Async
    public void updateProductAvailability(Long productId, boolean inStock, Integer stockQuantity) {
        try {
            Optional<ProductSearchDocument> existingDoc = productSearchRepository.findById(productId.toString());
            
            if (existingDoc.isPresent()) {
                ProductSearchDocument document = existingDoc.get();
                document.setInStock(inStock);
                document.setStockQuantity(stockQuantity);
                document.setLowStock(stockQuantity != null && stockQuantity < 10);
                document.setLastIndexed(LocalDateTime.now());
                
                productSearchRepository.save(document);
                
                log.debug("Updated availability for product {} in search index", productId);
            }
            
        } catch (Exception e) {
            log.error("Failed to update product availability in search index: {}", productId, e);
        }
    }
    
    /**
     * Update product pricing in search index
     */
    @Async
    public void updateProductPricing(Long productId, BigDecimal basePrice, BigDecimal salePrice) {
        try {
            Optional<ProductSearchDocument> existingDoc = productSearchRepository.findById(productId.toString());
            
            if (existingDoc.isPresent()) {
                ProductSearchDocument document = existingDoc.get();
                document.setBasePrice(basePrice);
                document.setSalePrice(salePrice);
                
                if (salePrice != null && salePrice.compareTo(basePrice) < 0) {
                    document.setOnSale(true);
                    document.setDiscountPercentage(
                        basePrice.subtract(salePrice)
                            .divide(basePrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .floatValue()
                    );
                } else {
                    document.setOnSale(false);
                    document.setDiscountPercentage(0f);
                }
                
                document.setLastIndexed(LocalDateTime.now());
                productSearchRepository.save(document);
                
                log.debug("Updated pricing for product {} in search index", productId);
            }
            
        } catch (Exception e) {
            log.error("Failed to update product pricing in search index: {}", productId, e);
        }
    }
    
    /**
     * Convert Product entity to ProductSearchDocument
     */
    private ProductSearchDocument convertToSearchDocument(Product product) {
        // Build autocomplete text
        String autocompleteText = buildAutocompleteText(product);
        
        // Build searchable text with synonyms
        String searchableText = buildSearchableText(product);
        
        // Extract product attributes
        Map<String, Object> customAttributes = extractCustomAttributes(product);
        
        // Calculate popularity score (this could be more sophisticated)
        Float popularityScore = calculatePopularityScore(product);
        
        // Build category hierarchy
        List<String> categoryHierarchy = buildCategoryHierarchy(product);
        
        // Build completion suggestions
        ProductSearchDocument.Completion completion = buildCompletionSuggestions(product);
        
        return ProductSearchDocument.builder()
            .id(product.getId().toString())
            .name(product.getName())
            .description(product.getDescription())
            .sku(product.getSku())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .categoryHierarchy(categoryHierarchy)
            .basePrice(product.getBasePrice())
            .salePrice(null) // This would come from a separate pricing entity
            .onSale(false)
            .discountPercentage(0f)
            .inStock(true) // This would come from inventory system
            .stockQuantity(100) // This would come from inventory system
            .lowStock(false)
            .brand(extractBrand(product))
            .manufacturer(extractManufacturer(product))
            .tags(extractTags(product))
            .colors(extractColors(product))
            .sizes(extractSizes(product))
            .material(extractMaterial(product))
            .averageRating(0f) // This would come from review aggregation
            .reviewCount(0)
            .imageUrls(product.getImages() != null ? product.getImages() : new ArrayList<>())
            .primaryImageUrl(extractPrimaryImageUrl(product))
            .metaDescription(product.getMetaDescription())
            .keywords(extractKeywords(product))
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .lastIndexed(LocalDateTime.now())
            .autocompleteText(autocompleteText)
            .searchableText(searchableText)
            .viewCount(0) // This would come from analytics
            .orderCount(0) // This would come from order history
            .popularityScore(popularityScore)
            .searchRankingScore(1.0f)
            .customAttributes(customAttributes)
            .variationIds(new ArrayList<>()) // This would come from product variations
            .parentProductId(null)
            .isFeatured(false) // This would be a product attribute
            .isNewArrival(isNewArrival(product))
            .isBestSeller(false) // This would come from sales data
            .location(null) // For future geo-search
            .isActive(true)
            .isVisible(true)
            .status("ACTIVE")
            .suggest(completion)
            .build();
    }
    
    /**
     * Build autocomplete text for the product
     */
    private String buildAutocompleteText(Product product) {
        StringBuilder autocomplete = new StringBuilder();
        
        if (product.getName() != null) {
            autocomplete.append(product.getName()).append(" ");
        }
        
        if (product.getSku() != null) {
            autocomplete.append(product.getSku()).append(" ");
        }
        
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            autocomplete.append(product.getCategory().getName()).append(" ");
        }
        
        // Add brand if available
        String brand = extractBrand(product);
        if (brand != null) {
            autocomplete.append(brand).append(" ");
        }
        
        return autocomplete.toString().trim();
    }
    
    /**
     * Build searchable text with potential synonyms
     */
    private String buildSearchableText(Product product) {
        StringBuilder searchable = new StringBuilder();
        
        if (product.getName() != null) {
            searchable.append(product.getName()).append(" ");
        }
        
        if (product.getDescription() != null) {
            searchable.append(product.getDescription()).append(" ");
        }
        
        if (product.getMetaDescription() != null) {
            searchable.append(product.getMetaDescription()).append(" ");
        }
        
        // Add extracted attributes
        List<String> tags = extractTags(product);
        if (tags != null) {
            searchable.append(String.join(" ", tags)).append(" ");
        }
        
        return searchable.toString().trim();
    }
    
    /**
     * Extract custom attributes from product
     */
    private Map<String, Object> extractCustomAttributes(Product product) {
        Map<String, Object> attributes = new HashMap<>();
        
        // This would be implemented based on your product attribute system
        // For now, return empty map
        return attributes;
    }
    
    /**
     * Calculate popularity score for search ranking
     */
    private Float calculatePopularityScore(Product product) {
        // This is a simplified popularity calculation
        // In a real system, this would consider:
        // - View count
        // - Order count
        // - Review count and ratings
        // - Recent activity
        // - Category popularity
        
        float score = 1.0f;
        
        // Boost newer products slightly
        if (product.getCreatedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime weekAgo = now.minusWeeks(1);
            
            if (product.getCreatedAt().isAfter(weekAgo)) {
                score *= 1.2f;
            }
        }
        
        return score;
    }
    
    /**
     * Build category hierarchy for faceted search
     */
    private List<String> buildCategoryHierarchy(Product product) {
        List<String> hierarchy = new ArrayList<>();
        
        if (product.getCategory() != null) {
            // This would build the full category path
            // For now, just add the immediate category
            hierarchy.add(product.getCategory().getName());
        }
        
        return hierarchy;
    }
    
    /**
     * Build completion suggestions for autocomplete
     */
    private ProductSearchDocument.Completion buildCompletionSuggestions(Product product) {
        List<String> inputs = new ArrayList<>();
        
        if (product.getName() != null) {
            inputs.add(product.getName());
            
            // Add individual words from the name
            String[] words = product.getName().split("\\s+");
            for (String word : words) {
                if (word.length() > 2) {
                    inputs.add(word);
                }
            }
        }
        
        if (product.getSku() != null) {
            inputs.add(product.getSku());
        }
        
        String brand = extractBrand(product);
        if (brand != null) {
            inputs.add(brand);
        }
        
        return ProductSearchDocument.Completion.builder()
            .input(inputs.toArray(new String[0]))
            .weight(calculateCompletionWeight(product))
            .contexts(new HashMap<>())
            .build();
    }
    
    /**
     * Calculate weight for completion suggestions
     */
    private Integer calculateCompletionWeight(Product product) {
        // Higher weight means higher priority in autocomplete
        // This could be based on popularity, sales, etc.
        return 10;
    }
    
    /**
     * Check if product is a new arrival
     */
    private Boolean isNewArrival(Product product) {
        if (product.getCreatedAt() == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoWeeksAgo = now.minusWeeks(2);
        
        return product.getCreatedAt().isAfter(twoWeeksAgo);
    }
    
    // Helper methods to extract product attributes
    // These would be implemented based on your actual product model
    
    private String extractBrand(Product product) {
        // Extract brand from product attributes or separate brand entity
        return null;
    }
    
    private String extractManufacturer(Product product) {
        // Extract manufacturer from product attributes
        return null;
    }
    
    private List<String> extractTags(Product product) {
        // Extract tags from product
        return new ArrayList<>();
    }
    
    private List<String> extractColors(Product product) {
        // Extract colors from product variations
        return new ArrayList<>();
    }
    
    private List<String> extractSizes(Product product) {
        // Extract sizes from product variations
        return new ArrayList<>();
    }
    
    private String extractMaterial(Product product) {
        // Extract material from product attributes
        return null;
    }
    
    private String extractPrimaryImageUrl(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0);
        }
        return null;
    }
    
    private List<String> extractKeywords(Product product) {
        // Extract SEO keywords
        return new ArrayList<>();
    }
}

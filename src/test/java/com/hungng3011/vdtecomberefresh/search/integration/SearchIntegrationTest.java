package com.hungng3011.vdtecomberefresh.search.integration;

import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchRequestDto;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchResponseDto;
import com.hungng3011.vdtecomberefresh.search.repositories.ProductSearchRepository;
import com.hungng3011.vdtecomberefresh.search.services.ProductSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for search functionality.
 * 
 * Note: These tests require Elasticsearch to be running.
 * Run `docker-compose up elasticsearch` before running these tests.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "elasticsearch.host=localhost",
    "elasticsearch.port=9200"
})
public class SearchIntegrationTest {
    
    @Autowired
    private ProductSearchService productSearchService;
    
    @Autowired
    private ProductSearchRepository productSearchRepository;
    
    private ProductSearchDocument testProduct1;
    private ProductSearchDocument testProduct2;
    private ProductSearchDocument testProduct3;
    
    @BeforeEach
    void setUp() {
        // Clean up existing test data
        productSearchRepository.deleteAll();
        
        // Create test products
        testProduct1 = ProductSearchDocument.builder()
            .id("test-1")
            .name("Gaming Laptop Dell XPS")
            .description("High-performance gaming laptop with NVIDIA graphics")
            .sku("DELL-XPS-001")
            .categoryId(1L)
            .categoryName("Electronics")
            .basePrice(BigDecimal.valueOf(1299.99))
            .inStock(true)
            .stockQuantity(10)
            .brand("Dell")
            .tags(Arrays.asList("gaming", "laptop", "nvidia"))
            .averageRating(4.5f)
            .reviewCount(25)
            .imageUrls(Arrays.asList("/images/dell-xps-1.jpg", "/images/dell-xps-2.jpg"))
            .primaryImageUrl("/images/dell-xps-1.jpg")
            .autocompleteText("Gaming Laptop Dell XPS")
            .searchableText("Gaming Laptop Dell XPS High-performance gaming laptop with NVIDIA graphics")
            .popularityScore(8.5f)
            .searchRankingScore(9.0f)
            .isActive(true)
            .isVisible(true)
            .isFeatured(true)
            .isNewArrival(false)
            .status("ACTIVE")
            .createdAt(LocalDateTime.now().minusDays(30))
            .updatedAt(LocalDateTime.now())
            .lastIndexed(LocalDateTime.now())
            .build();
            
        testProduct2 = ProductSearchDocument.builder()
            .id("test-2")
            .name("iPhone 15 Pro")
            .description("Latest iPhone with advanced camera system")
            .sku("APPLE-IP15-PRO")
            .categoryId(2L)
            .categoryName("Mobile Phones")
            .basePrice(BigDecimal.valueOf(999.99))
            .salePrice(BigDecimal.valueOf(899.99))
            .onSale(true)
            .discountPercentage(10f)
            .inStock(true)
            .stockQuantity(50)
            .brand("Apple")
            .tags(Arrays.asList("smartphone", "iphone", "camera"))
            .averageRating(4.8f)
            .reviewCount(150)
            .imageUrls(Arrays.asList("/images/iphone-15-1.jpg"))
            .primaryImageUrl("/images/iphone-15-1.jpg")
            .autocompleteText("iPhone 15 Pro Apple")
            .searchableText("iPhone 15 Pro Apple Latest iPhone with advanced camera system")
            .popularityScore(9.8f)
            .searchRankingScore(9.5f)
            .isActive(true)
            .isVisible(true)
            .isFeatured(true)
            .isNewArrival(true)
            .status("ACTIVE")
            .createdAt(LocalDateTime.now().minusDays(7))
            .updatedAt(LocalDateTime.now())
            .lastIndexed(LocalDateTime.now())
            .build();
            
        testProduct3 = ProductSearchDocument.builder()
            .id("test-3")
            .name("Samsung 4K TV")
            .description("Ultra HD Smart TV with HDR support")
            .sku("SAMSUNG-TV-4K")
            .categoryId(3L)
            .categoryName("Electronics")
            .basePrice(BigDecimal.valueOf(799.99))
            .inStock(false)
            .stockQuantity(0)
            .lowStock(true)
            .brand("Samsung")
            .tags(Arrays.asList("tv", "4k", "smart"))
            .averageRating(4.2f)
            .reviewCount(80)
            .imageUrls(Arrays.asList("/images/samsung-tv-1.jpg"))
            .primaryImageUrl("/images/samsung-tv-1.jpg")
            .autocompleteText("Samsung 4K TV")
            .searchableText("Samsung 4K TV Ultra HD Smart TV with HDR support")
            .popularityScore(7.5f)
            .searchRankingScore(8.0f)
            .isActive(true)
            .isVisible(true)
            .isFeatured(false)
            .isNewArrival(false)
            .status("ACTIVE")
            .createdAt(LocalDateTime.now().minusDays(60))
            .updatedAt(LocalDateTime.now())
            .lastIndexed(LocalDateTime.now())
            .build();
        
        // Index test products
        productSearchRepository.saveAll(Arrays.asList(testProduct1, testProduct2, testProduct3));
        
        // Wait for indexing to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    void testBasicSearch() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("laptop")
            .searchType(SearchRequestDto.SearchType.FUZZY)
            .page(0)
            .size(10)
            .sortBy("relevance")
            .sortDirection(SearchRequestDto.SortDirection.DESC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().size() > 0);
        
        // Should find the gaming laptop
        boolean foundLaptop = response.getResults().stream()
            .anyMatch(product -> product.getName().contains("Gaming Laptop"));
        assertTrue(foundLaptop);
        
        // Check metadata
        assertNotNull(response.getMetadata());
        assertTrue(response.getMetadata().getTotalHits() > 0);
        assertEquals(0, response.getMetadata().getPage());
        assertEquals(10, response.getMetadata().getSize());
    }
    
    @Test
    void testSearchWithFilters() {
        SearchRequestDto.SearchFilters filters = SearchRequestDto.SearchFilters.builder()
            .categoryIds(Arrays.asList(1L, 3L)) // Electronics
            .minPrice(BigDecimal.valueOf(500))
            .maxPrice(BigDecimal.valueOf(1500))
            .brands(Arrays.asList("Dell", "Samsung"))
            .inStock(true)
            .build();
        
        SearchRequestDto request = SearchRequestDto.builder()
            .query("gaming")
            .searchType(SearchRequestDto.SearchType.MULTI_MATCH)
            .filters(filters)
            .page(0)
            .size(10)
            .sortBy("price")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertNotNull(response.getResults());
        
        // Should only find products matching filters
        response.getResults().forEach(product -> {
            assertTrue(product.getBasePrice().compareTo(BigDecimal.valueOf(500)) >= 0);
            assertTrue(product.getBasePrice().compareTo(BigDecimal.valueOf(1500)) <= 0);
            assertTrue(Arrays.asList("Dell", "Samsung").contains(product.getBrand()));
            assertTrue(product.getInStock());
        });
    }
    
    @Test
    void testPriceRangeSearch() {
        SearchRequestDto.SearchFilters filters = SearchRequestDto.SearchFilters.builder()
            .minPrice(BigDecimal.valueOf(800))
            .maxPrice(BigDecimal.valueOf(1000))
            .build();
        
        SearchRequestDto request = SearchRequestDto.builder()
            .query("*") // All products
            .filters(filters)
            .page(0)
            .size(10)
            .sortBy("price")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        
        // Should find products in price range
        response.getResults().forEach(product -> {
            assertTrue(product.getBasePrice().compareTo(BigDecimal.valueOf(800)) >= 0);
            assertTrue(product.getBasePrice().compareTo(BigDecimal.valueOf(1000)) <= 0);
        });
    }
    
    @Test
    void testBrandFilter() {
        SearchRequestDto.SearchFilters filters = SearchRequestDto.SearchFilters.builder()
            .brands(Arrays.asList("Apple"))
            .build();
        
        SearchRequestDto request = SearchRequestDto.builder()
            .query("phone")
            .filters(filters)
            .page(0)
            .size(10)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        
        // Should only find Apple products
        response.getResults().forEach(product -> {
            assertEquals("Apple", product.getBrand());
        });
    }
    
    @Test
    void testSuggestions() {
        List<SearchResponseDto.SearchSuggestion> suggestions = 
            productSearchService.getSuggestions("gam", 5);
        
        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
        
        // Should find gaming laptop suggestion
        boolean foundGamingSuggestion = suggestions.stream()
            .anyMatch(suggestion -> suggestion.getText().toLowerCase().contains("gaming"));
        assertTrue(foundGamingSuggestion);
    }
    
    @Test
    void testFacets() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("*")
            .page(0)
            .size(10)
            .includeFacets(true)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertNotNull(response.getFacets());
        
        // Should have category and brand facets
        assertTrue(response.getFacets().containsKey("categories"));
        assertTrue(response.getFacets().containsKey("brands"));
    }
    
    @Test
    void testSortingByPrice() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("*")
            .page(0)
            .size(10)
            .sortBy("price")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertTrue(response.getResults().size() > 1);
        
        // Should be sorted by price ascending
        for (int i = 1; i < response.getResults().size(); i++) {
            BigDecimal prevPrice = response.getResults().get(i - 1).getBasePrice();
            BigDecimal currentPrice = response.getResults().get(i).getBasePrice();
            assertTrue(prevPrice.compareTo(currentPrice) <= 0);
        }
    }
    
    @Test
    void testSortingByName() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("*")
            .page(0)
            .size(10)
            .sortBy("name")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertTrue(response.getResults().size() > 1);
        
        // Should be sorted by name ascending
        for (int i = 1; i < response.getResults().size(); i++) {
            String prevName = response.getResults().get(i - 1).getName();
            String currentName = response.getResults().get(i).getName();
            assertTrue(prevName.compareToIgnoreCase(currentName) <= 0);
        }
    }
    
    @Test
    void testPagination() {
        // Test first page
        SearchRequestDto request1 = SearchRequestDto.builder()
            .query("*")
            .page(0)
            .size(2)
            .sortBy("name")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response1 = productSearchService.search(request1);
        
        assertNotNull(response1);
        assertEquals(2, response1.getResults().size());
        assertEquals(0, response1.getMetadata().getPage());
        assertTrue(response1.getMetadata().getHasNext());
        assertFalse(response1.getMetadata().getHasPrevious());
        
        // Test second page
        SearchRequestDto request2 = SearchRequestDto.builder()
            .query("*")
            .page(1)
            .size(2)
            .sortBy("name")
            .sortDirection(SearchRequestDto.SortDirection.ASC)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response2 = productSearchService.search(request2);
        
        assertNotNull(response2);
        assertTrue(response2.getResults().size() > 0);
        assertEquals(1, response2.getMetadata().getPage());
        assertTrue(response2.getMetadata().getHasPrevious());
        
        // Results should be different
        assertNotEquals(response1.getResults().get(0).getId(), response2.getResults().get(0).getId());
    }
    
    @Test
    void testEmptyQuery() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("")
            .page(0)
            .size(10)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertNotNull(response.getResults());
        // Should return all products when query is empty
        assertEquals(3, response.getResults().size());
    }
    
    @Test
    void testNoResultsQuery() {
        SearchRequestDto request = SearchRequestDto.builder()
            .query("nonexistentproduct12345")
            .page(0)
            .size(10)
            .build();
        
        SearchResponseDto<ProductSearchDocument> response = productSearchService.search(request);
        
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(0, response.getResults().size());
        assertEquals(0L, response.getMetadata().getTotalHits());
    }
}

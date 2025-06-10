package com.hungng3011.vdtecomberefresh.product.controllers;

import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryRepository;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.product.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductPaginationIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setImageUrl("test-category-image.jpg");
        testCategory = categoryRepository.save(testCategory);

        // Create test products
        testProducts = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Product product = new Product();
            product.setName("Test Product " + i);
            product.setDescription("Test Description " + i);
            product.setBasePrice(BigDecimal.valueOf(100 + i));
            product.setCategory(testCategory);
            testProducts.add(product);
        }
        testProducts = productRepository.saveAll(testProducts);
    }

    @Test
    void testBasicPaginationWithoutCursor() {
        // Test first page without cursor
        PagedResponse<ProductDto> response = productService.getByCategoryIdWithPagination(
                testCategory.getId(), 0, 5, null);

        assertNotNull(response);
        assertEquals(5, response.getContent().size());
        assertEquals(15, response.getPagination().getTotalElements());
        assertEquals(3, response.getPagination().getTotalPages());
        assertEquals(0, response.getPagination().getPage());
        assertEquals(5, response.getPagination().getSize());
        assertTrue(response.getPagination().isHasNext());
        assertFalse(response.getPagination().isHasPrevious());
        assertNotNull(response.getPagination().getNextCursor());
        assertNull(response.getPagination().getPreviousCursor());
    }

    @Test
    void testPaginationWithCursor() {
        // Get first page
        PagedResponse<ProductDto> firstPage = productService.getByCategoryIdWithPagination(
                testCategory.getId(), 0, 5, null);
        
        Long firstPageNextCursor = (Long) firstPage.getPagination().getNextCursor();
        assertNotNull(firstPageNextCursor);

        // Get second page using cursor
        PagedResponse<ProductDto> secondPage = productService.getByCategoryIdWithPagination(
                testCategory.getId(), 1, 5, firstPageNextCursor);

        assertNotNull(secondPage);
        assertEquals(5, secondPage.getContent().size());
        assertTrue(secondPage.getPagination().isHasNext());
        assertTrue(secondPage.getPagination().isHasPrevious());
        assertNotNull(secondPage.getPagination().getNextCursor());
        assertEquals(firstPageNextCursor, secondPage.getPagination().getPreviousCursor());

        // Verify that the products are different between pages
        List<Long> firstPageIds = firstPage.getContent().stream()
                .map(ProductDto::getId)
                .toList();
        List<Long> secondPageIds = secondPage.getContent().stream()
                .map(ProductDto::getId)
                .toList();

        // Ensure no overlap between pages
        firstPageIds.retainAll(secondPageIds);
        assertTrue(firstPageIds.isEmpty(), "Pages should not have overlapping products");
    }

    @Test
    void testLastPage() {
        // Get last page (page 2, which should have 5 products)
        PagedResponse<ProductDto> lastPage = productService.getByCategoryIdWithPagination(
                testCategory.getId(), 2, 5, null);

        assertNotNull(lastPage);
        assertEquals(5, lastPage.getContent().size());
        assertFalse(lastPage.getPagination().isHasNext());
        assertTrue(lastPage.getPagination().isHasPrevious());
        assertNull(lastPage.getPagination().getNextCursor());
        assertNotNull(lastPage.getPagination().getPreviousCursor());
    }

    @Test
    void testPreviousCursorPagination() {
        // Get second page
        PagedResponse<ProductDto> secondPage = productService.getByCategoryIdWithPagination(
                testCategory.getId(), 1, 5, null);
        
        Long cursor = (Long) secondPage.getPagination().getNextCursor();
        assertNotNull(cursor);

        // Get previous page using cursor
        PagedResponse<ProductDto> previousPage = productService.getByCategoryIdWithPreviousCursor(
                testCategory.getId(), 1, 5, cursor);

        assertNotNull(previousPage);
        assertEquals(5, previousPage.getContent().size());
        assertTrue(previousPage.getPagination().isHasNext());
        assertTrue(previousPage.getPagination().isHasPrevious());
        assertEquals(cursor, previousPage.getPagination().getNextCursor());
        assertNotNull(previousPage.getPagination().getPreviousCursor());
    }

    @Test
    void testEmptyResultsForNonExistentCategory() {
        // Test with non-existent category
        PagedResponse<ProductDto> response = productService.getByCategoryIdWithPagination(
                999L, 0, 5, null);

        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        assertEquals(0, response.getPagination().getTotalElements());
        assertEquals(0, response.getPagination().getTotalPages());
        assertFalse(response.getPagination().isHasNext());
        assertFalse(response.getPagination().isHasPrevious());
        assertNull(response.getPagination().getNextCursor());
        assertNull(response.getPagination().getPreviousCursor());
    }
}

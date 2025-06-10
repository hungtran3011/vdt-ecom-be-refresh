package com.hungng3011.vdtecomberefresh.product.controllers;

import com.hungng3011.vdtecomberefresh.product.dtos.*;
import com.hungng3011.vdtecomberefresh.product.services.*;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public PagedResponse<ProductDto> get(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long cursor) {
        log.info("Fetching paginated products - page: {}, size: {}, cursor: {}", page, size, cursor);
        try {
            PagedResponse<ProductDto> response = productService.getAllWithPagination(page, size, cursor);
            log.info("Successfully retrieved {} products (page: {})", 
                    response.getContent().size(), page);
            return response;
        } catch (Exception e) {
            log.error("Error fetching paginated products", e);
            throw e;
        }
    }

    @GetMapping("/previous")
    public PagedResponse<ProductDto> getAllProductsWithPreviousCursor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long cursor) {
        log.info("Fetching previous page of products with cursor: {}", cursor);
        try {
            PagedResponse<ProductDto> response = productService.getAllWithPreviousCursor(page, size, cursor);
            log.info("Successfully retrieved {} products for previous page", 
                    response.getContent().size());
            return response;
        } catch (Exception e) {
            log.error("Error fetching previous page of products", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        log.info("Fetching product with ID: {}", id);
        try {
            ProductDto product = productService.getById(id);
            if (product == null) {
                log.warn("Product not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Successfully retrieved product with ID: {}", id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("Error fetching product with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDto> getByCategoryId(@PathVariable Long categoryId) {
        log.info("Fetching products for category ID: {}", categoryId);
        try {
            List<ProductDto> products = productService.getByCategoryId(categoryId);
            log.info("Successfully retrieved {} products for category ID: {}", products.size(), categoryId);
            return products;
        } catch (Exception e) {
            log.error("Error fetching products for category ID: {}", categoryId, e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductDto req) {
        log.info("Creating new product: {}", req.getName());
        try {
            ProductDto result = productService.create(req);
            if (result == null) {
                log.warn("Failed to create product: {}", req.getName());
                return ResponseEntity.badRequest().build();
            }
            log.info("Successfully created product with ID: {} and name: {}", result.getId(), result.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating product: {}", req.getName(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@Valid @RequestBody ProductDto req) {
        log.info("Updating product with ID: {} and name: {}", req.getId(), req.getName());
        try {
            ProductDto result = productService.update(req);
            if (result == null) {
                log.warn("Failed to update product with ID: {}", req.getId());
                return ResponseEntity.notFound().build();
            }
            log.info("Successfully updated product with ID: {}", result.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating product with ID: {}", req.getId(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting product with ID: {}", id);
        try {
            if (productService.getById(id) == null) {
                log.warn("Product not found for deletion with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            productService.delete(id);
            log.info("Successfully deleted product with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting product with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/category/{categoryId}/paginated")
    public PagedResponse<ProductDto> getByCategoryIdWithPagination(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long cursor) {
        log.info("Fetching paginated products for category ID: {} - page: {}, size: {}, cursor: {}", 
                categoryId, page, size, cursor);
        try {
            PagedResponse<ProductDto> response = productService.getByCategoryIdWithPagination(categoryId, page, size, cursor);
            log.info("Successfully retrieved {} products for category ID: {} (page: {})", 
                    response.getContent().size(), categoryId, page);
            return response;
        } catch (Exception e) {
            log.error("Error fetching paginated products for category ID: {}", categoryId, e);
            throw e;
        }
    }

    @GetMapping("/category/{categoryId}/paginated/previous")
    public PagedResponse<ProductDto> getByCategoryIdWithPreviousCursor(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long cursor) {
        log.info("Fetching previous page of products for category ID: {} with cursor: {}", categoryId, cursor);
        try {
            PagedResponse<ProductDto> response = productService.getByCategoryIdWithPreviousCursor(categoryId, page, size, cursor);
            log.info("Successfully retrieved {} products for previous page of category ID: {}", 
                    response.getContent().size(), categoryId);
            return response;
        } catch (Exception e) {
            log.error("Error fetching previous page of products for category ID: {}", categoryId, e);
            throw e;
        }
    }


}


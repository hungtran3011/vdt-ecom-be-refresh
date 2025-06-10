package com.hungng3011.vdtecomberefresh.product.services;

import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryRepository;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.product.dtos.VariationDynamicValueDto;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.entities.ProductDynamicValue;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import com.hungng3011.vdtecomberefresh.product.mappers.ProductMapper;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.stock.StockService;
import com.hungng3011.vdtecomberefresh.exception.product.ProductProcessingException;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import org.springframework.cache.annotation.Cacheable;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;
    private final StockService stockService;

    public List<ProductDto> getAll() {
        log.info("Fetching all products");
        try {
            List<ProductDto> products = productRepository.findAll().stream().map(productMapper::toDto)
                    .collect(Collectors.toList());
            log.info("Successfully retrieved {} products", products.size());
            return products;
        } catch (Exception e) {
            log.error("Error fetching all products", e);
            throw e;
        }
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(Long id) {
        log.info("Get product by id: {}", id);
        try {
            ProductDto product = productRepository.findById(id).map(productMapper::toDto).orElse(null);
            if (product != null) {
                log.info("Successfully retrieved product with id: {}", id);
            } else {
                log.warn("Product not found with id: {}", id);
            }
            return product;
        } catch (Exception e) {
            log.error("Error retrieving product with id: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public ProductDto create(ProductDto request) {
        log.info("Creating product: {} with category ID: {}", request.getName(), request.getCategoryId());
        try {
            // 1. Create and save the base product first
            Product product = new Product();
            product.setName(request.getName());
            product.setDescription(request.getDescription());
            product.setBasePrice(request.getBasePrice());
            product.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());

            // Set category
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {}", request.getCategoryId());
                        return new ProductProcessingException("Category not found", request.getCategoryId());
                    });
            product.setCategory(category);

            // Initialize collections to avoid NPEs
            product.setDynamicValues(new ArrayList<>());
            product.setVariations(new ArrayList<>());

            // Save product first to get ID
            Product savedProduct = productRepository.saveAndFlush(product);
            log.debug("Saved base product with ID: {}", savedProduct.getId());

            // 2. Handle product dynamic values
            if (request.getDynamicValues() != null) {
                log.debug("Processing {} dynamic values for product: {}", request.getDynamicValues().size(),
                        savedProduct.getName());
                for (ProductDynamicValueDto dynamicValueDto : request.getDynamicValues()) {
                    // Verify the field exists in category_dynamic_fields
                    Long fieldId = dynamicValueDto.getField().getId();
                    CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                    if (field == null) {
                        log.error("Category dynamic field not found with ID: {}, cleaning up product", fieldId);
                        productRepository.delete(savedProduct); // Clean up saved product to prevent orphaned records
                        throw new ProductProcessingException("CATEGORY_FIELD_NOT_FOUND",
                                "Category dynamic field not found", fieldId);
                    }

                    // Create dynamic value
                    var dynamicValue = new ProductDynamicValue();
                    dynamicValue.setProduct(savedProduct);
                    dynamicValue.setField(field);
                    dynamicValue.setValue(dynamicValueDto.getValue());

                    entityManager.persist(dynamicValue);
                    savedProduct.getDynamicValues().add(dynamicValue);
                }
                entityManager.flush();
                log.debug("Successfully processed dynamic values for product: {}", savedProduct.getName());
            }

            // 3. Handle variations
            if (request.getVariations() != null) {
                for (VariationDto variationDto : request.getVariations()) {
                    Variation variation = new Variation();
                    variation.setProduct(savedProduct);
                    variation.setName(variationDto.getName());
                    variation.setType(variationDto.getType());
                    variation.setAdditionalPrice(variationDto.getAdditionalPrice());
                    variation.setDynamicValues(new ArrayList<>());

                    entityManager.persist(variation);
                    savedProduct.getVariations().add(variation);

                    // Process variation dynamic values
                    if (variationDto.getDynamicValues() != null) {
                        for (VariationDynamicValueDto dynamicValueDto : variationDto.getDynamicValues()) {
                            // Verify field exists
                            Long fieldId = dynamicValueDto.getField().getId();
                            CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                            if (field == null) {
                                productRepository.delete(savedProduct); // Clean up saved product to prevent orphaned records
                                throw new ProductProcessingException("CATEGORY_FIELD_NOT_FOUND",
                                        "Category dynamic field not found", fieldId);
                            }

                            // Create dynamic value
                            var dynamicValue = new VariationDynamicValue();
                            dynamicValue.setVariation(variation);
                            dynamicValue.setField(field);
                            dynamicValue.setValue(dynamicValueDto.getValue());

                            entityManager.persist(dynamicValue);
                            variation.getDynamicValues().add(dynamicValue);
                        }
                    }
                }
                entityManager.flush();
            }

            // Reload the complete product with all relationships
            Product finalProduct = productRepository.findById(savedProduct.getId())
                    .orElseThrow(() -> new ProductProcessingException("PRODUCT_NOT_FOUND",
                            "Product not found after saving", savedProduct.getId()));
            return productMapper.toDto(finalProduct);
        } catch (Exception e) {
            log.error("Error creating product: {}", request.getName(), e);
            throw new ProductProcessingException("PRODUCT_CREATION_FAILED",
                    "Failed to create product", e);
        }
    }

    @Transactional
    public ProductDto update(ProductDto request) {
        log.info("Starting product update for ID: {} with name: {}", request.getId(), request.getName());
        
        try {
            if (request.getId() == null) {
                log.error("Product update failed: Product ID is required");
                throw new ProductProcessingException("PRODUCT_ID_REQUIRED",
                        "Product ID is required for update");
            }

            // Fetch the existing product
            log.debug("Fetching existing product with ID: {}", request.getId());
            Product existingProduct = productRepository.findById(request.getId())
                    .orElseThrow(() -> {
                        log.error("Product not found for update with ID: {}", request.getId());
                        return new ProductProcessingException("PRODUCT_NOT_FOUND",
                                "Product not found", request.getId());
                    });

            log.debug("Found existing product: {} - updating basic information", existingProduct.getName());
            // Update basic product information
            existingProduct.setName(request.getName());
            existingProduct.setDescription(request.getDescription());
            existingProduct.setBasePrice(request.getBasePrice());
            existingProduct.setImages(request.getImages() != null ? request.getImages() : existingProduct.getImages());

            // Update category if changed
            if (request.getCategoryId() != null) {
                log.debug("Updating category for product ID: {} to category ID: {}", request.getId(), request.getCategoryId());
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> {
                            log.error("Category not found for update with ID: {}", request.getCategoryId());
                            return new ProductProcessingException("CATEGORY_NOT_FOUND",
                                    "Category not found", request.getCategoryId());
                        });
                existingProduct.setCategory(category);
            }

            // Save updates to base product
            log.debug("Saving basic product updates for ID: {}", request.getId());
            Product updatedProduct = productRepository.saveAndFlush(existingProduct);

            // Handle dynamic values - clear and recreate approach
            if (request.getDynamicValues() != null) {
                log.debug("Updating {} dynamic values for product ID: {}", request.getDynamicValues().size(), request.getId());
                
                // Clear existing dynamic values
                for (ProductDynamicValue dynamicValue : new ArrayList<>(updatedProduct.getDynamicValues())) {
                    entityManager.remove(dynamicValue);
                }
                updatedProduct.getDynamicValues().clear();
                entityManager.flush();

                // Create new dynamic values
                for (ProductDynamicValueDto dynamicValueDto : request.getDynamicValues()) {
                    Long fieldId = dynamicValueDto.getField().getId();
                    log.trace("Processing dynamic value for field ID: {}", fieldId);
                    
                    CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);
                    if (field == null) {
                        log.error("Category dynamic field not found with ID: {} during product update", fieldId);
                        throw new ProductProcessingException("CATEGORY_FIELD_NOT_FOUND",
                                "Category dynamic field not found", fieldId);
                    }

                    ProductDynamicValue dynamicValue = new ProductDynamicValue();
                    dynamicValue.setProduct(updatedProduct);
                    dynamicValue.setField(field);
                    dynamicValue.setValue(dynamicValueDto.getValue());

                    entityManager.persist(dynamicValue);
                    updatedProduct.getDynamicValues().add(dynamicValue);
                }
                entityManager.flush();
                log.debug("Successfully updated dynamic values for product ID: {}", request.getId());
            }

            // Handle variations - clear and recreate approach
            if (request.getVariations() != null) {
                log.debug("Updating {} variations for product ID: {}", request.getVariations().size(), request.getId());
                
                // Clear existing variations and their dynamic values
                for (Variation variation : new ArrayList<>(updatedProduct.getVariations())) {
                    if (variation.getDynamicValues() != null) {
                        for (VariationDynamicValue dynamicValue : new ArrayList<>(variation.getDynamicValues())) {
                            entityManager.remove(dynamicValue);
                        }
                        variation.getDynamicValues().clear();
                    }
                    entityManager.remove(variation);
                }
                updatedProduct.getVariations().clear();
                entityManager.flush();

                // Create new variations
                for (VariationDto vDto : request.getVariations()) {
                    log.trace("Processing variation: {} of type: {}", vDto.getName(), vDto.getType());
                    
                    Variation variation = new Variation();
                    variation.setProduct(updatedProduct);
                    variation.setName(vDto.getName());
                    variation.setType(vDto.getType());
                    variation.setAdditionalPrice(vDto.getAdditionalPrice());
                    variation.setDynamicValues(new ArrayList<>());

                    entityManager.persist(variation);
                    updatedProduct.getVariations().add(variation);

                    // Process variation dynamic values
                    if (vDto.getDynamicValues() != null) {
                        log.trace("Processing {} dynamic values for variation: {}", vDto.getDynamicValues().size(), vDto.getName());
                        for (VariationDynamicValueDto dynamicValueDto : vDto.getDynamicValues()) {
                            Long fieldId = dynamicValueDto.getField().getId();
                            CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                            if (field == null) {
                                log.error("Category dynamic field not found with ID: {} for variation dynamic value", fieldId);
                                throw new ProductProcessingException("CATEGORY_FIELD_NOT_FOUND",
                                        "Category dynamic field not found", fieldId);
                            }

                            VariationDynamicValue dynamicValue = new VariationDynamicValue();
                            dynamicValue.setVariation(variation);
                            dynamicValue.setField(field);
                            dynamicValue.setValue(dynamicValueDto.getValue());

                            entityManager.persist(dynamicValue);
                            variation.getDynamicValues().add(dynamicValue);
                        }
                    }
                }
                entityManager.flush();
                log.debug("Successfully updated variations for product ID: {}", request.getId());
            }

            // Reload the complete product with all relationships
            log.debug("Reloading updated product with ID: {} to get complete relationships", updatedProduct.getId());
            Product finalProduct = productRepository.findById(updatedProduct.getId())
                    .orElseThrow(() -> {
                        log.error("Product not found after update with ID: {}", updatedProduct.getId());
                        return new ProductProcessingException("PRODUCT_NOT_FOUND",
                                "Product not found after update", updatedProduct.getId());
                    });

            log.info("Successfully updated product with ID: {} and name: {}", finalProduct.getId(), finalProduct.getName());
            return productMapper.toDto(finalProduct);
            
        } catch (ProductProcessingException e) {
            log.error("Product processing error during update for ID: {}", request.getId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating product with ID: {}", request.getId(), e);
            throw new ProductProcessingException("PRODUCT_UPDATE_FAILED",
                    "Failed to update product", e);
        }
    }

    @Transactional
    public void delete(Long id) {
        log.info("Starting product deletion for ID: {}", id);
        
        try {
            log.debug("Fetching product for deletion with ID: {}", id);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Product not found for deletion with ID: {}", id);
                        return new ProductProcessingException("PRODUCT_NOT_FOUND", "Product not found", id);
                    });

            log.debug("Found product for deletion: {}", product.getName());

            // Remove all product dynamic values
            if (product.getDynamicValues() != null) {
                log.debug("Removing {} dynamic values for product ID: {}", product.getDynamicValues().size(), id);
                product.getDynamicValues().forEach(entityManager::remove);
                product.getDynamicValues().clear();
            }

            // Remove all variation dynamic values and variations
            if (product.getVariations() != null) {
                log.debug("Removing {} variations for product ID: {}", product.getVariations().size(), id);
                for (Variation variation : new ArrayList<>(product.getVariations())) {
                    if (variation.getDynamicValues() != null) {
                        log.trace("Removing {} dynamic values for variation: {}", variation.getDynamicValues().size(), variation.getName());
                        variation.getDynamicValues().forEach(entityManager::remove);
                        variation.getDynamicValues().clear();
                    }
                    entityManager.remove(variation);
                }
                product.getVariations().clear();
            }

            // Remove stock entries related to this product
            log.debug("Removing stock entries for product ID: {}", id);
            stockService.removeStockByProductId(id);

            // Delete the product
            log.debug("Deleting product entity with ID: {}", id);
            productRepository.delete(product);
            log.info("Successfully deleted product with ID: {}", id);
            
        } catch (ProductProcessingException e) {
            log.error("Product processing error during deletion for ID: {}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting product with ID: {}", id, e);
            throw new ProductProcessingException("PRODUCT_DELETION_FAILED",
                    "Failed to delete product", e);
        }
    }

    public List<ProductDto> getByCategoryId(Long categoryId) {
        log.info("Finding products by category ID: {}", categoryId);
        
        try {
            log.debug("Fetching category with ID: {}", categoryId);
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                log.warn("Category with ID {} not found, returning empty list", categoryId);
                return List.of();
            }
            
            log.debug("Found category: {}, fetching products", category.getName());
            List<ProductDto> products = productRepository.findByCategory(category).stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());
            
            log.info("Successfully retrieved {} products for category ID: {}", products.size(), categoryId);
            return products;
            
        } catch (Exception e) {
            log.error("Error retrieving products for category ID: {}", categoryId, e);
            throw new ProductProcessingException("PRODUCT_RETRIEVAL_FAILED",
                    "Failed to retrieve products by category", e);
        }
    }

    /**
     * Get products by category with cursor-based pagination
     * @param categoryId The category ID
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing products and pagination metadata
     */
    public PagedResponse<ProductDto> getByCategoryIdWithPagination(Long categoryId, int page, int size, Long cursor) {
        log.info("Finding products by category ID: {} with pagination - page: {}, size: {}, cursor: {}", 
                categoryId, page, size, cursor);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductProcessingException("CATEGORY_NOT_FOUND", 
                        "Category not found", categoryId));

        Pageable pageable = PageRequest.of(0, size); // We handle page logic manually with cursor
        List<Product> products;
        
        if (cursor != null) {
            // Use cursor-based pagination
            products = productRepository.findByCategoryWithCursorAfter(category, cursor, pageable);
        } else {
            // First page - use standard pagination
            Page<Product> productPage = productRepository.findByCategory(category, PageRequest.of(page, size));
            products = productPage.getContent();
        }

        List<ProductDto> productDtos = products.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());

        // Get total count for pagination metadata
        long totalElements = productRepository.countByCategory(category);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Calculate cursors
        Long nextCursor = null;
        Long previousCursor = null;
        
        if (!products.isEmpty()) {
            nextCursor = products.get(products.size() - 1).getId();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        // Build pagination metadata
        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(products.size() == size && (page + 1) * size < totalElements)
                .hasPrevious(page > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} products for category ID: {} (page: {}, total: {})", 
                productDtos.size(), categoryId, page, totalElements);

        return PagedResponse.<ProductDto>builder()
                .content(productDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get products by category with previous page cursor
     * @param categoryId The category ID
     * @param page Page number (0-based) 
     * @param size Number of items per page
     * @param cursor Cursor for going to previous page
     * @return PagedResponse containing products and pagination metadata
     */
    public PagedResponse<ProductDto> getByCategoryIdWithPreviousCursor(Long categoryId, int page, int size, Long cursor) {
        log.info("Finding previous products by category ID: {} with cursor: {} (page: {}, size: {})", 
                categoryId, cursor, page, size);
        
        try {
            log.debug("Fetching category with ID: {}", categoryId);
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {} for previous page query", categoryId);
                        return new ProductProcessingException("CATEGORY_NOT_FOUND", 
                                "Category not found", categoryId);
                    });

            log.debug("Found category: {}, fetching previous products with cursor: {}", category.getName(), cursor);
            Pageable pageable = PageRequest.of(0, size);
            List<Product> products = productRepository.findByCategoryWithCursorBefore(category, cursor, pageable);
            
            // Reverse the order since we queried in DESC order
            java.util.Collections.reverse(products);

            List<ProductDto> productDtos = products.stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());

            long totalElements = productRepository.countByCategory(category);
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Long nextCursor = cursor;
            Long previousCursor = !products.isEmpty() ? products.get(0).getId() : null;

            PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                    .page(Math.max(0, page - 1))
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(true)
                    .hasPrevious(products.size() == size)
                    .nextCursor(nextCursor)
                    .previousCursor(previousCursor)
                    .build();

            log.info("Successfully retrieved {} previous products for category ID: {} (page: {}, total: {})", 
                    productDtos.size(), categoryId, page, totalElements);

            return PagedResponse.<ProductDto>builder()
                    .content(productDtos)
                    .pagination(pagination)
                    .build();
                    
        } catch (ProductProcessingException e) {
            log.error("Product processing error during previous page query for category ID: {}", categoryId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving previous products for category ID: {}", categoryId, e);
            throw new ProductProcessingException("PRODUCT_RETRIEVAL_FAILED",
                    "Failed to retrieve previous products by category", e);
        }
    }

    /**
     * Get all products with cursor-based pagination
     * @param page Page number (for metadata calculation)
     * @param size Number of items per page
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing products and pagination metadata
     */
    public PagedResponse<ProductDto> getAllWithPagination(int page, int size, Long cursor) {
        log.info("Finding all products with pagination - page: {}, size: {}, cursor: {}", 
                page, size, cursor);
        
        try {
            Pageable pageable = PageRequest.of(0, size); // We handle page logic manually with cursor
            List<Product> products;
            
            if (cursor != null) {
                log.debug("Using cursor-based pagination with cursor: {}", cursor);
                // Use cursor-based pagination
                products = productRepository.findAllWithCursorAfter(cursor, pageable);
            } else {
                log.debug("Using first page pagination without cursor");
                // First page - use standard pagination
                products = productRepository.findAllWithCursorAfter(null, pageable);
            }

            List<ProductDto> productDtos = products.stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());

            // Get total count for pagination metadata
            long totalElements = productRepository.countAllProducts();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // Calculate cursors
            Long nextCursor = null;
            Long previousCursor = null;
            
            if (!products.isEmpty()) {
                nextCursor = products.get(products.size() - 1).getId();
                if (cursor != null) {
                    previousCursor = cursor;
                }
            }

            // Build pagination metadata
            PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(products.size() == size && (page + 1) * size < totalElements)
                    .hasPrevious(page > 0 || cursor != null)
                    .nextCursor(nextCursor)
                    .previousCursor(previousCursor)
                    .build();

            log.info("Successfully retrieved {} products (page: {}, total: {})", 
                    productDtos.size(), page, totalElements);

            return PagedResponse.<ProductDto>builder()
                    .content(productDtos)
                    .pagination(pagination)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error retrieving all products with pagination (page: {}, size: {}, cursor: {})", 
                    page, size, cursor, e);
            throw new ProductProcessingException("PRODUCT_RETRIEVAL_FAILED",
                    "Failed to retrieve products with pagination", e);
        }
    }

    /**
     * Get all products with cursor-based pagination (previous page)
     * @param page Page number (for metadata calculation)
     * @param size Number of items per page
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing products and pagination metadata
     */
    public PagedResponse<ProductDto> getAllWithPreviousCursor(int page, int size, Long cursor) {
        log.info("Finding previous products with cursor: {} (page: {}, size: {})", cursor, page, size);
        
        try {
            Pageable pageable = PageRequest.of(0, size);
            log.debug("Fetching products before cursor: {}", cursor);
            List<Product> products = productRepository.findAllWithCursorBefore(cursor, pageable);
            
            // Reverse the order since we queried in DESC order
            java.util.Collections.reverse(products);

            List<ProductDto> productDtos = products.stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());

            long totalElements = productRepository.countAllProducts();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Long nextCursor = cursor;
            Long previousCursor = !products.isEmpty() ? products.get(0).getId() : null;

            PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                    .page(Math.max(0, page - 1))
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(true)
                    .hasPrevious(products.size() == size)
                    .nextCursor(nextCursor)
                    .previousCursor(previousCursor)
                    .build();

            log.info("Successfully retrieved {} previous products (page: {}, total: {})", 
                    productDtos.size(), page, totalElements);

            return PagedResponse.<ProductDto>builder()
                    .content(productDtos)
                    .pagination(pagination)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error retrieving previous products with cursor: {} (page: {}, size: {})", 
                    cursor, page, size, e);
            throw new ProductProcessingException("PRODUCT_RETRIEVAL_FAILED",
                    "Failed to retrieve previous products", e);
        }
    }
}

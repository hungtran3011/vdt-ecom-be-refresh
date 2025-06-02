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
import org.springframework.cache.annotation.Cacheable;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;
    private final StockService stockService;

    public List<ProductDto> getAll() {
        return productRepository.findAll().stream().map(productMapper::toDto).collect(Collectors.toList());
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(Long id) {
        logger.info("Get product by id: {}", id);
        return productRepository.findById(id).map(productMapper::toDto).orElse(null);
    }

    @Transactional
    public ProductDto create(ProductDto request) {
        logger.info("Creating product: {}", request);

        // 1. Create and save the base product first
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());

        // Set category
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + request.getCategoryId()));
        product.setCategory(category);

        // Initialize collections to avoid NPEs
        product.setDynamicValues(new ArrayList<>());
        product.setVariations(new ArrayList<>());

        // Save product first to get ID
        Product savedProduct = productRepository.saveAndFlush(product);
        logger.debug("Saved base product with ID: {}", savedProduct.getId());

        // 2. Handle product dynamic values
        if (request.getDynamicValues() != null) {
            for (ProductDynamicValueDto dynamicValueDto : request.getDynamicValues()) {
                // Verify the field exists in category_dynamic_fields
                Long fieldId = dynamicValueDto.getField().getId();
                CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                if (field == null) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category dynamic field not found with id: " + fieldId
                    );
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
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Category dynamic field not found with id: " + fieldId
                            );
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
            .orElseThrow(() -> new IllegalStateException("Product not found after saving"));
        return productMapper.toDto(finalProduct);
    }

    @Transactional
    public ProductDto update(ProductDto request) {
        logger.info("Updating product: {}", request);

        if (request.getId() == null) {
            throw new IllegalArgumentException("Product ID is required for update");
        }

        // Fetch the existing product
        Product existingProduct = productRepository.findById(request.getId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found with id: " + request.getId()
            ));

        // Update basic product information
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setBasePrice(request.getBasePrice());
        existingProduct.setImages(request.getImages() != null ? request.getImages() : existingProduct.getImages());

        // Update category if changed
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found with id: " + request.getCategoryId()
                ));
            existingProduct.setCategory(category);
        }

        // Save updates to base product
        existingProduct = productRepository.saveAndFlush(existingProduct);

        // Handle dynamic values - clear and recreate approach
        if (request.getDynamicValues() != null) {
            // Clear existing values
            existingProduct.getDynamicValues().clear();
            entityManager.flush();

            // Add new values
            for (ProductDynamicValueDto dvDto : request.getDynamicValues()) {
                Long fieldId = dvDto.getField().getId();
                CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                if (field == null) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category dynamic field not found with id: " + fieldId
                    );
                }

                var dynamicValue = new ProductDynamicValue();
                dynamicValue.setProduct(existingProduct);
                dynamicValue.setField(field);
                dynamicValue.setValue(dvDto.getValue());

                entityManager.persist(dynamicValue);
                existingProduct.getDynamicValues().add(dynamicValue);
            }
            entityManager.flush();
        }

        // Handle variations - clear and recreate approach
        if (request.getVariations() != null) {
            // Remove all existing variations
            for (Variation variation : new ArrayList<>(existingProduct.getVariations())) {
                entityManager.remove(variation);
            }
            existingProduct.getVariations().clear();
            entityManager.flush();

            // Create new variations
            for (VariationDto vDto : request.getVariations()) {
                Variation variation = new Variation();
                variation.setProduct(existingProduct);
                variation.setName(vDto.getName());
                variation.setType(vDto.getType());
                variation.setAdditionalPrice(vDto.getAdditionalPrice());
                variation.setDynamicValues(new ArrayList<>());

                entityManager.persist(variation);
                existingProduct.getVariations().add(variation);

                // Add variation dynamic values
                if (vDto.getDynamicValues() != null) {
                    for (VariationDynamicValueDto dvDto : vDto.getDynamicValues()) {
                        Long fieldId = dvDto.getField().getId();
                        CategoryDynamicField field = entityManager.find(CategoryDynamicField.class, fieldId);

                        if (field == null) {
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Category dynamic field not found with id: " + fieldId
                            );
                        }

                        var dynamicValue = new VariationDynamicValue();
                        dynamicValue.setVariation(variation);
                        dynamicValue.setField(field);
                        dynamicValue.setValue(dvDto.getValue());

                        entityManager.persist(dynamicValue);
                        variation.getDynamicValues().add(dynamicValue);
                    }
                }
            }
            entityManager.flush();
        }

        // Reload the complete product with all relationships
        Product finalProduct = productRepository.findById(existingProduct.getId())
            .orElseThrow(() -> new IllegalStateException("Product not found after update"));

        return productMapper.toDto(finalProduct);
    }

    public void delete(Long id) {
       logger.info("Deleting product by id: {}", id);
       Product product = productRepository.findById(id)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));

       // Remove all product dynamic values
       if (product.getDynamicValues() != null) {
           product.getDynamicValues().forEach(entityManager::remove);
           product.getDynamicValues().clear();
       }

       // Remove all variation dynamic values and variations
       if (product.getVariations() != null) {
           for (Variation variation : new ArrayList<>(product.getVariations())) {
               if (variation.getDynamicValues() != null) {
                   variation.getDynamicValues().forEach(entityManager::remove);
                   variation.getDynamicValues().clear();
               }
               entityManager.remove(variation);
           }
           product.getVariations().clear();
       }

       // Remove stock entries related to this product
       stockService.removeStockByProductId(id);

       // Delete the product
       productRepository.delete(product);
       logger.info("Product with id {} deleted successfully", id);
    }

    public List<ProductDto> getByCategoryId(Long categoryId) {
        logger.info("Finding products by category ID: {}", categoryId);
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            logger.warn("Category with ID {} not found", categoryId);
            return List.of();
        }
        return productRepository.findByCategory(category).stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }
}

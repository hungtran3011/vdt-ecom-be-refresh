package com.hungng3011.vdtecomberefresh.category;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDto;
import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import com.hungng3011.vdtecomberefresh.category.mappers.CategoryDynamicFieldMapper;
import com.hungng3011.vdtecomberefresh.category.mappers.CategoryMapper;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryDynamicFieldRepository;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryRepository;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hungng3011.vdtecomberefresh.exception.category.CategoryProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryDynamicFieldRepository categoryDynamicFieldRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryDynamicFieldMapper categoryDynamicFieldMapper;
    private final EntityManager entityManager;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final HttpCodeStatusMapper httpCodeStatusMapper;
    private final ProductRepository productRepository;

    @Cacheable(value = "categories")
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryDto getById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto)
                .orElse(null);
    }

    @Transactional
//    @Cacheable(value = "categories", key = "#result.id")
    public CategoryDto create(CategoryDto dto) {
        // Create and save basic category first
        Category category = new Category();
        category.setName(dto.getName());
        category = categoryRepository.saveAndFlush(category);

        // Create dynamic fields list for return value
        List<CategoryDynamicField> savedFields = new ArrayList<>();

        // Handle dynamic fields if present
        if (dto.getDynamicFields() != null && !dto.getDynamicFields().isEmpty()) {
            for (CategoryDynamicFieldDto fieldDto : dto.getDynamicFields()) {
                // Create and directly persist each field
                CategoryDynamicField field = new CategoryDynamicField();
                field.setFieldName(fieldDto.getFieldName());
                field.setFieldType(fieldDto.getFieldType());
                field.setAppliesTo(fieldDto.getAppliesTo());
                field.setCategory(category);

                // Directly persist each field using EntityManager
                entityManager.persist(field);
                entityManager.flush();

                savedFields.add(field);
            }
        }

        // Set the fields collection on category
        category.setDynamicFields(savedFields);
        CategoryDto result = categoryMapper.toDto(categoryRepository.save(category));
        return result;
    }

    public CategoryDto update(CategoryDto dto) {
        Category category = categoryRepository.findById(dto.getId()).orElse(null);
        if (category == null) {
            return null;
        }
        List<Product> products = productRepository.findByCategory(category);
        if (products != null && !products.isEmpty()) {
            logger.warn("Cannot update category with id {} because it is associated with products", dto.getId());
            throw new CategoryProcessingException("CATEGORY_HAS_PRODUCTS", 
                "Category is associated with products and cannot be updated", dto.getId());
        }
        CategoryDto categoryDto = categoryMapper.toDto(category);
        category.setName(dto.getName());

        // Update dynamic fields
        List<CategoryDynamicFieldDto> requestFields = dto.getDynamicFields();
        List<CategoryDynamicFieldDto> existingFields = category.getDynamicFields() != null
                ? category.getDynamicFields().stream().map(categoryDynamicFieldMapper::toDto).toList()
                : List.of();

        // Delete fields not in request
        if (category.getDynamicFields() != null) {
            category.getDynamicFields().removeIf(existingField ->
                    requestFields == null || requestFields.stream().noneMatch(reqField ->
                            reqField.getId() != null && reqField.getId().equals(existingField.getId())));
        }

        // Add or update fields from request
        if (requestFields != null) {
            requestFields.forEach(reqField -> {
                if (reqField.getId() == null) {
                    // New field
                    reqField.setCategoryId(categoryDto.getId());
                    category.getDynamicFields().add(categoryDynamicFieldMapper.toEntity(reqField));
                } else {
                    // Update existing field
                    category.getDynamicFields().stream()
                            .filter(existingField -> reqField.getId().equals(existingField.getId()))
                            .findFirst()
                            .ifPresent(existingField -> {
                                existingField.setFieldName(reqField.getFieldName());
                                existingField.setFieldType(reqField.getFieldType());
                                existingField.setAppliesTo(reqField.getAppliesTo());
                                existingField.setRequired(reqField.isRequired());
                                existingField.setCategory(category);
                            });
                }
            });
        }

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", key = "#id")
    public void delete(Long id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category != null) {
            // Remove dynamic fields first
            if (category.getDynamicFields() != null) {
                category.getDynamicFields().forEach(field -> {
                    categoryDynamicFieldRepository.deleteById(field.getId()); // Remove the field
                });
            }
            categoryRepository.delete(category);
        } else {
            logger.warn("Category with id {} not found for deletion", id);
            throw new CategoryProcessingException("CATEGORY_NOT_FOUND", "Category not found", id);
        }
    }
}


package com.hungng3011.vdtecomberefresh.product.validators;

import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import com.hungng3011.vdtecomberefresh.category.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductValidator {

    public static void validate(ProductDto productDto, Category category) {
        if (productDto == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        // Validate variations have base price
        validateVariations(productDto.getVariations());

        // Validate required fields
        validateRequiredFields(productDto, category);
    }

    private static void validateVariations(List<VariationDto> variations) {
        if (variations == null || variations.isEmpty()) {
            return;
        }

        // Group variations by type
        Map<String, List<VariationDto>> variationsByType = variations.stream()
                .filter(v -> v.getType() != null)
                .collect(Collectors.groupingBy(VariationDto::getType));

        // Check each type has at least one base variation with additionalPrice = 0
        for (Map.Entry<String, List<VariationDto>> entry : variationsByType.entrySet()) {
            String type = entry.getKey();
            List<VariationDto> typeVariations = entry.getValue();

            boolean hasBaseVariation = typeVariations.stream()
                    .anyMatch(v -> v.getAdditionalPrice() != null &&
                                 v.getAdditionalPrice().compareTo(BigDecimal.ZERO) == 0);

            if (!hasBaseVariation) {
                throw new IllegalArgumentException("Variation type '" + type +
                        "' must have at least one base variation with additionalPrice = 0");
            }
        }
    }

    private static void validateRequiredFields(ProductDto productDto, Category category) {
        if (category.getDynamicFields() == null || category.getDynamicFields().isEmpty()) {
            return;
        }

        // Get all required product fields
        List<CategoryDynamicField> requiredProductFields = category.getDynamicFields().stream()
                .filter(field -> field.isRequired() &&
                               field.getAppliesTo() == AppliesTo.PRODUCT)
                .collect(Collectors.toList());

        if (!requiredProductFields.isEmpty()) {
            // Get all field IDs from product dynamic values
            Set<Long> providedFieldIds = Collections.emptySet();
            if (productDto.getDynamicValues() != null) {
                providedFieldIds = productDto.getDynamicValues().stream()
                        .filter(dv -> dv.getField() != null && dv.getField().getId() != null)
                        .map(dv -> dv.getField().getId())
                        .collect(Collectors.toSet());
            }

            // Check all required fields are present
            for (CategoryDynamicField requiredField : requiredProductFields) {
                if (!providedFieldIds.contains(requiredField.getId())) {
                    throw new IllegalArgumentException("Required field '" + requiredField.getFieldName() +
                            "' is missing in product dynamic values");
                }
            }
        }

        // Validate variations if present
        if (productDto.getVariations() != null && !productDto.getVariations().isEmpty()) {
            validateVariationRequiredFields(productDto.getVariations(), category);
        }
    }

    private static void validateVariationRequiredFields(List<VariationDto> variations, Category category) {
        // Get all required variation fields
        List<CategoryDynamicField> requiredVariationFields = category.getDynamicFields().stream()
                .filter(field -> field.isRequired() &&
                               field.getAppliesTo() == AppliesTo.VARIATION)
                .collect(Collectors.toList());

        if (requiredVariationFields.isEmpty()) {
            return;
        }

        // Check each variation has all required fields
        for (VariationDto variation : variations) {
            // Get field IDs in this variation
            Set<Long> providedFieldIds = Collections.emptySet();
            if (variation.getDynamicValues() != null) {
                providedFieldIds = variation.getDynamicValues().stream()
                        .filter(dv -> dv.getField() != null && dv.getField().getId() != null)
                        .map(dv -> dv.getField().getId())
                        .collect(Collectors.toSet());
            }

            // Check all required fields are present
            for (CategoryDynamicField requiredField : requiredVariationFields) {
                if (!providedFieldIds.contains(requiredField.getId())) {
                    String variationName = variation.getName() != null ? variation.getName() : "unnamed";
                    throw new IllegalArgumentException("Required field '" + requiredField.getFieldName() +
                            "' is missing in variation '" + variationName + "'");
                }
            }
        }
    }
}
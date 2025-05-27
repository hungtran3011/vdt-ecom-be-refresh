package com.hungng3011.vdtecomberefresh.product;

import com.hungng3011.vdtecomberefresh.product.dtos.ProductDto;
import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import com.hungng3011.vdtecomberefresh.product.repositories.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryDynamicFieldRepository categoryDynamicFieldRepository;
    private final ProductDynamicValueRepository productDynamicValueRepository;
    private final VariationRepository variationRepository;
    private final VariationDynamicValueRepository variationDynamicValueRepository;

    public ProductDto create(ProductDto product) {
        return productRepository.save(product);
    }
}

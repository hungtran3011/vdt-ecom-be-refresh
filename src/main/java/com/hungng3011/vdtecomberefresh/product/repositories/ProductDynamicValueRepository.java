package com.hungng3011.vdtecomberefresh.product.repositories;

import com.hungng3011.vdtecomberefresh.product.entities.ProductDynamicValue;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDynamicValueRepository extends JpaRepository<ProductDynamicValue, Long> {
    List<ProductDynamicValue> getProductDynamicValuesByField_Id(Long fieldId, Limit limit);
}

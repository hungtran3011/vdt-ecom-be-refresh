package com.hungng3011.vdtecomberefresh.product.repositories;

import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariationDynamicValueRepository extends JpaRepository<VariationDynamicValue, Long> {
    List<VariationDynamicValue> getVariationDynamicValuesByField_Id(Long fieldId, Limit limit);
}

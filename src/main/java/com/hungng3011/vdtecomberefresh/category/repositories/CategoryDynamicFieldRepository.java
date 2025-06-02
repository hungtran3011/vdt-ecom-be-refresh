package com.hungng3011.vdtecomberefresh.category.repositories;

import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryDynamicFieldRepository extends JpaRepository<CategoryDynamicField, Long> {
    Optional<CategoryDynamicField> findByCategoryIdAndFieldName(Long categoryId, String fieldName);
}

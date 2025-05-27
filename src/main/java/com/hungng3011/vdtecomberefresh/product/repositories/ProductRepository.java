package com.hungng3011.vdtecomberefresh.product.repositories;

import com.hungng3011.vdtecomberefresh.product.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}

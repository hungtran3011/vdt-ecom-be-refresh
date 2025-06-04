package com.hungng3011.vdtecomberefresh.cart.repositories;

import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}

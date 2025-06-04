package com.hungng3011.vdtecomberefresh.order.repositories;

import com.hungng3011.vdtecomberefresh.order.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}

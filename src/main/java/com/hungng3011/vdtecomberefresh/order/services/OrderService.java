package com.hungng3011.vdtecomberefresh.order.services;

import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        Order order = orderMapper.toEntity(orderDto);
        order.setId(UUID.randomUUID().toString());
        String currentTime = Instant.now().toString();
        order.setCreatedAt(currentTime);
        order.setUpdatedAt(currentTime);

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order); // Set bidirectional relationship
            }
        }
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto updateOrder(String id, OrderDto orderDto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        // Use mapper to update fields, excluding id and createdAt
        orderMapper.updateEntityFromDto(orderDto, existingOrder);
        existingOrder.setUpdatedAt(Instant.now().toString());

        // Handle items update: clear existing and add new ones
        // This assumes full replacement of items. More sophisticated merging might be needed.
        if (existingOrder.getItems() != null) {
            existingOrder.getItems().clear();
        }
        if (orderDto.getItems() != null && !orderDto.getItems().isEmpty()) {
            List<OrderItem> newItems = orderMapper.toEntity(orderDto).getItems();
            if (newItems != null) {
                newItems.forEach(item -> {
                    item.setOrder(existingOrder); // Set bidirectional relationship
                    existingOrder.getItems().add(item);
                });
            }
        }

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public void deleteOrder(String id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }
}
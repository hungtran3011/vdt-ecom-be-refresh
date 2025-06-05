package com.hungng3011.vdtecomberefresh.order.services;

import com.hungng3011.vdtecomberefresh.mail.services.NotificationService;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final ProfileService profileService;

    /**
     * Helper method to get customer email from profile
     */
    private String getCustomerEmail(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            ProfileDto profile = profileService.getProfile(userUuid);
            return profile.getEmail();
        } catch (Exception e) {
            log.warn("Failed to get customer email for userId: {}", userId, e);
            return null;
        }
    }

    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        Order order = orderMapper.toEntity(orderDto);
        order.setId(UUID.randomUUID().toString());
        // String currentTime = Instant.now().toString();
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order); // Set bidirectional relationship
            }
        }
        Order savedOrder = orderRepository.save(order);
        OrderDto savedOrderDto = orderMapper.toDto(savedOrder);
        
        // Send order confirmation email asynchronously
        try {
            String customerEmail = getCustomerEmail(order.getUserId());
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                log.info("Sending order confirmation email for order: {}", savedOrderDto.getId());
                notificationService.sendOrderConfirmationEmail(savedOrderDto, customerEmail);
            } else {
                log.warn("No email address found for user: {}, skipping email notification for order: {}", 
                        order.getUserId(), savedOrderDto.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", savedOrderDto.getId(), e);
            // Don't fail the order creation if email fails
        }
        
        return savedOrderDto;
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
        existingOrder.setUpdatedAt(LocalDateTime.now());

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
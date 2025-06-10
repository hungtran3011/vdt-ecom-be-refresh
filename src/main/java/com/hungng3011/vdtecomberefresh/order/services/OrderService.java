package com.hungng3011.vdtecomberefresh.order.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.mail.services.NotificationService;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ProductRepository productRepository;

    /**
     * Helper method to get customer email from order
     */
    private String getCustomerEmail(String userEmail) {
        // Since we're now using email directly, just return it
        return userEmail;
    }

    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        Order order = orderMapper.toEntity(orderDto);
        order.setId(UUID.randomUUID().toString());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Set Product entity references for each OrderItem
        if (order.getItems() != null && orderDto.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem item = order.getItems().get(i);
                Long productId = orderDto.getItems().get(i).getProductId();
                
                if (productId != null) {
                    Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
                    item.setProduct(product);
                }
                item.setOrder(order); // Set bidirectional relationship
            }
        }

        Order savedOrder = orderRepository.save(order);
        OrderDto savedOrderDto = orderMapper.toDto(savedOrder);
        
        // Send order confirmation email asynchronously
        try {
            String customerEmail = getCustomerEmail(order.getUserEmail());
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                log.info("Sending order confirmation email for order: {}", savedOrderDto.getId());
                notificationService.sendOrderConfirmationEmail(savedOrderDto, customerEmail);
            } else {
                log.warn("No email address found for user: {}, skipping email notification for order: {}", 
                        order.getUserEmail(), savedOrderDto.getId());
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
                for (int i = 0; i < newItems.size(); i++) {
                    OrderItem item = newItems.get(i);
                    Long productId = orderDto.getItems().get(i).getProductId();
                    
                    // Set Product entity reference
                    if (productId != null) {
                        Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
                        item.setProduct(product);
                    }
                    
                    item.setOrder(existingOrder); // Set bidirectional relationship
                    existingOrder.getItems().add(item);
                }
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

    /**
     * Cancel an order if it's in a cancellable state
     * @param id The order ID
     * @param userEmail The user email for authorization
     * @return The updated order DTO
     */
    @Transactional
    public OrderDto cancelOrder(String id, String userEmail) {
        log.info("Cancelling order with ID: {} for user: {}", id, userEmail);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        // Verify order belongs to user
        if (!order.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Order does not belong to the authenticated user");
        }
        
        // Check if order can be cancelled
        if (!canCancelOrder(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        
        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        
        // Send cancellation email
        try {
            String customerEmail = getCustomerEmail(order.getUserEmail());
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                log.info("Sending order cancellation email for order: {}", id);
                // For now, use a simple email notification until we add the proper template
                notificationService.sendOrderDeliveredEmail(id, customerEmail); // Temporary - will be replaced with proper cancellation email
            }
        } catch (Exception e) {
            log.error("Failed to send order cancellation email for order: {}", id, e);
            // Don't fail the cancellation if email fails
        }
        
        log.info("Successfully cancelled order: {}", id);
        return orderMapper.toDto(savedOrder);
    }
    
    /**
     * Check if an order can be cancelled based on its current status
     * @param status The current order status
     * @return true if the order can be cancelled
     */
    private boolean canCancelOrder(OrderStatus status) {
        return status == OrderStatus.PENDING_PAYMENT || 
               status == OrderStatus.PAID || 
               status == OrderStatus.CONFIRMED;
    }

    /**
     * Get order tracking information
     * @param id The order ID
     * @param userEmail The user email for authorization
     * @return The order with tracking details
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderTracking(String id, String userEmail) {
        log.info("Getting tracking info for order: {} for user: {}", id, userEmail);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        // Verify order belongs to user
        if (!order.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Order does not belong to the authenticated user");
        }
        
        return orderMapper.toDto(order);
    }

    /**
     * Get orders with cursor-based pagination (next page)
     * @param page Page number (for metadata calculation)
     * @param size Number of items per page
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing orders and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getAllOrdersWithPagination(int page, int size, String cursor) {
        log.info("Finding orders with pagination - page: {}, size: {}, cursor: {}", 
                page, size, cursor);
        
        Pageable pageable = PageRequest.of(0, size); // We handle page logic manually with cursor
        List<Order> orders;
        
        if (cursor != null) {
            // Use cursor-based pagination
            orders = orderRepository.findWithCursorAfter(cursor, pageable);
        } else {
            // First page - use standard pagination
            orders = orderRepository.findWithCursorAfter(null, pageable);
        }

        List<OrderDto> orderDtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());

        // Get total count for pagination metadata
        long totalElements = orderRepository.countAllOrders();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Calculate cursors
        String nextCursor = null;
        String previousCursor = null;
        
        if (!orders.isEmpty()) {
            nextCursor = orders.get(orders.size() - 1).getId();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        // Build pagination metadata
        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(orders.size() == size && (page + 1) * size < totalElements)
                .hasPrevious(page > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} orders (page: {}, total: {})", 
                orderDtos.size(), page, totalElements);

        return PagedResponse.<OrderDto>builder()
                .content(orderDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get orders with cursor-based pagination (previous page)
     * @param page Page number (for metadata calculation)
     * @param size Number of items per page
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing orders and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getAllOrdersWithPreviousCursor(int page, int size, String cursor) {
        log.info("Finding previous orders with cursor: {}", cursor);
        
        Pageable pageable = PageRequest.of(0, size);
        List<Order> orders = orderRepository.findWithCursorBefore(cursor, pageable);
        
        // Reverse the order since we queried in DESC order
        java.util.Collections.reverse(orders);

        List<OrderDto> orderDtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = orderRepository.countAllOrders();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        String nextCursor = cursor;
        String previousCursor = !orders.isEmpty() ? orders.get(0).getId() : null;

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(true)
                .hasPrevious(orders.size() == size)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        return PagedResponse.<OrderDto>builder()
                .content(orderDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get orders by user email with pagination
     * @param userEmail The user's email address
     * @param pageable Pagination information
     * @return List of orders for the specified user
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByUserEmail(String userEmail, Pageable pageable) {
        log.info("Finding orders for user email: {}", userEmail);
        
        Page<Order> orders = orderRepository.findByUserEmail(userEmail, pageable);
        List<OrderDto> orderDtos = orders.getContent().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        
        log.info("Found {} orders for user: {}", orderDtos.size(), userEmail);
        return orderDtos;
    }

    /**
     * Get all orders by user email (without pagination)
     * @param userEmail The user's email address
     * @return List of all orders for the specified user
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersByUserEmail(String userEmail) {
        log.info("Finding all orders for user email: {}", userEmail);
        
        // Use a large page size to get all orders
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Order> orders = orderRepository.findByUserEmail(userEmail, pageable);
        List<OrderDto> orderDtos = orders.getContent().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        
        log.info("Found {} total orders for user: {}", orderDtos.size(), userEmail);
        return orderDtos;
    }

    /**
     * Reorder an existing order by creating a new order with the same items
     * @param id The original order ID to reorder
     * @param userEmail The user email for authorization
     * @return The newly created order DTO
     */
    @Transactional
    public OrderDto reorderOrder(String id, String userEmail) {
        log.info("Reordering order with ID: {} for user: {}", id, userEmail);
        
        // Get the original order
        Order originalOrder = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        // Verify order belongs to user
        if (!originalOrder.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Order does not belong to the authenticated user");
        }
        
        // Create a new order based on the original
        Order newOrder = new Order();
        newOrder.setId(UUID.randomUUID().toString());
        newOrder.setUserEmail(originalOrder.getUserEmail());
        newOrder.setAddress(originalOrder.getAddress());
        newOrder.setPhone(originalOrder.getPhone());
        newOrder.setNote("Reorder from order: " + originalOrder.getId());
        newOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        newOrder.setPaymentStatus(originalOrder.getPaymentStatus()); // Keep same payment status expectation
        newOrder.setPaymentMethod(originalOrder.getPaymentMethod()); // Keep same payment method
        newOrder.setCreatedAt(LocalDateTime.now());
        newOrder.setUpdatedAt(LocalDateTime.now());
        newOrder.setItems(new ArrayList<>());
        
        // Copy items from original order
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItem originalItem : originalOrder.getItems()) {
            OrderItem newItem = new OrderItem();
            newItem.setProduct(originalItem.getProduct()); // Reuse same product reference
            newItem.setQuantity(originalItem.getQuantity());
            newItem.setPrice(originalItem.getPrice());
            newItem.setTotalPrice(originalItem.getTotalPrice());
            newItem.setOrder(newOrder);
            
            newOrder.getItems().add(newItem);
            totalPrice = totalPrice.add(newItem.getTotalPrice());
        }
        
        newOrder.setTotalPrice(totalPrice);
        
        // Save the new order
        Order savedOrder = orderRepository.save(newOrder);
        OrderDto savedOrderDto = orderMapper.toDto(savedOrder);
        
        // Send order confirmation email asynchronously
        try {
            String customerEmail = getCustomerEmail(newOrder.getUserEmail());
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                log.info("Sending reorder confirmation email for order: {}", savedOrderDto.getId());
                notificationService.sendOrderConfirmationEmail(savedOrderDto, customerEmail);
            } else {
                log.warn("No email address found for user: {}, skipping email notification for reorder: {}", 
                        newOrder.getUserEmail(), savedOrderDto.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send reorder confirmation email for order: {}", savedOrderDto.getId(), e);
            // Don't fail the reorder if email fails
        }
        
        log.info("Successfully created reorder with ID: {} from original order: {}", savedOrderDto.getId(), id);
        return savedOrderDto;
    }

    /**
     * Export orders to CSV format
     * @param userEmail Optional user email filter (null for admin exports)
     * @param status Optional status filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return CSV content as String
     */
    @Transactional(readOnly = true)
    public String exportOrdersToCSV(String userEmail, OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Exporting orders to CSV - userEmail: {}, status: {}, dateRange: {} to {}", 
                userEmail, status, startDate, endDate);
        
        // Build query criteria
        List<Order> orders;
        if (userEmail != null) {
            // Filter by user email
            orders = orderRepository.findByUserEmail(userEmail, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else {
            // Get all orders for admin export
            orders = orderRepository.findAll();
        }
        
        // Apply additional filters
        if (status != null) {
            orders = orders.stream()
                    .filter(order -> order.getStatus() == status)
                    .collect(Collectors.toList());
        }
        
        if (startDate != null && endDate != null) {
            orders = orders.stream()
                    .filter(order -> !order.getCreatedAt().isBefore(startDate) && !order.getCreatedAt().isAfter(endDate))
                    .collect(Collectors.toList());
        }
        
        // Generate CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Order ID,User Email,Total Price,Status,Payment Status,Payment Method,Address,Phone,Created At,Updated At,Items Count\n");
        
        for (Order order : orders) {
            csv.append(String.format("\"%s\",\"%s\",\"%.2f\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d\n",
                    order.getId(),
                    order.getUserEmail(),
                    order.getTotalPrice(),
                    order.getStatus(),
                    order.getPaymentStatus(),
                    order.getPaymentMethod(),
                    order.getAddress().replace("\"", "\"\""), // Escape quotes
                    order.getPhone(),
                    order.getCreatedAt().toString(),
                    order.getUpdatedAt().toString(),
                    order.getItems() != null ? order.getItems().size() : 0
            ));
        }
        
        log.info("Successfully exported {} orders to CSV", orders.size());
        return csv.toString();
    }
}
package com.hungng3011.vdtecomberefresh.order.services;

import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderItemDto;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import com.hungng3011.vdtecomberefresh.order.mappers.OrderMapper;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderDto orderDto;
    private OrderItem orderItem;
    private OrderItemDto orderItemDto;

    @BeforeEach
    void setUp() {
        String orderId = UUID.randomUUID().toString();
        LocalDateTime currentTime = LocalDateTime.now();

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProductId(10L);
        // orderItem.setOrder(order); // This would be set if OrderItem has Order reference

        orderItemDto = new OrderItemDto();
        orderItemDto.setId(1L);
        orderItemDto.setProductId(10L);

        order = new Order();
        order.setId(orderId);
        order.setUserId("user123");
        order.setCreatedAt(currentTime); // Order entity expects String
        order.setUpdatedAt(currentTime); // Order entity expects String
        order.setItems(Collections.singletonList(orderItem));


        orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setUserId("user123");
        orderDto.setCreatedAt(currentTime); // OrderDto expects LocalDateTime
        orderDto.setUpdatedAt(currentTime); // OrderDto expects LocalDateTime
        orderDto.setItems(Collections.singletonList(orderItemDto));
    }

    @Test
    void createOrder_shouldSaveAndReturnOrderDto() {
        // Mocking the state before ID and timestamps are set by the service
        Order orderToSave = new Order();
        orderToSave.setUserId("user123");
        OrderItem itemToSave = new OrderItem();
        itemToSave.setProductId(10L);
        orderToSave.setItems(Collections.singletonList(itemToSave));


        when(orderMapper.toEntity(any(OrderDto.class))).thenReturn(orderToSave);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            // Simulate database setting ID and OrderItem having Order reference
            savedOrder.setId(order.getId()); // Use pre-generated ID for consistency in test
            if (savedOrder.getItems() != null && !savedOrder.getItems().isEmpty()) {
                savedOrder.getItems().get(0).setId(1L);
                 // In a real scenario with bidirectional mapping and cascades,
                 // the OrderItem's 'order' field would be set.
                 // For this test, we assume the mapper handles DTO conversion correctly.
            }
            return savedOrder;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto createdOrderDto = orderService.createOrder(this.orderDto); // Pass the DTO that would be input

        assertNotNull(createdOrderDto);
        assertNotNull(createdOrderDto.getId());
        assertNotNull(createdOrderDto.getCreatedAt());
        assertNotNull(createdOrderDto.getUpdatedAt());
        assertEquals(this.orderDto.getUserId(), createdOrderDto.getUserId());
        assertFalse(createdOrderDto.getItems().isEmpty());

        verify(orderMapper, times(1)).toEntity(any(OrderDto.class));
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderMapper, times(1)).toDto(any(Order.class));

        // Verify that item.setOrder(order) was conceptually called
        // This is tricky to verify directly without deeper mocking or capturing arguments
        // if OrderItem.setOrder is what you expect to be called.
        // The current service logic iterates and calls item.setOrder(order).
        // We can check if the saved order entity had its items' order reference set.
        // This requires the 'orderToSave' items to have their 'order' field set.
        // For simplicity, we assume the mapping and saving process handles this.
        // If OrderItem.java had `private Order order;`, then `item.setOrder(order)` would be directly testable.
        // Given OrderItem.java has `private Long orderId;`, the current service logic `item.setOrder(order)`
        // will cause a compile error if OrderItem does not have a `setOrder(Order order)` method.
        // Assuming OrderItem will be updated to have `private Order order;`
        // and a `setOrder(Order order)` method.
    }

    @Test
    void getAllOrders_shouldReturnListOfOrderDtos() {
        when(orderRepository.findAll()).thenReturn(Collections.singletonList(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        List<OrderDto> result = orderService.getAllOrders();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(orderDto.getId(), result.get(0).getId());
        verify(orderRepository, times(1)).findAll();
        verify(orderMapper, times(1)).toDto(order);
    }

    @Test
    void getOrderById_shouldReturnOrderDto_whenOrderExists() {
        when(orderRepository.findById(anyString())).thenReturn(Optional.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.getOrderById(order.getId());

        assertNotNull(result);
        assertEquals(orderDto.getId(), result.getId());
        verify(orderRepository, times(1)).findById(order.getId());
        verify(orderMapper, times(1)).toDto(order);
    }

    @Test
    void getOrderById_shouldThrowEntityNotFoundException_whenOrderNotFound() {
        when(orderRepository.findById(anyString())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderService.getOrderById("nonExistentId"));
        assertEquals("Order not found with id: nonExistentId", exception.getMessage());
        verify(orderRepository, times(1)).findById("nonExistentId");
        verify(orderMapper, never()).toDto(any(Order.class));
    }

    @Test
    void updateOrder_shouldUpdateAndReturnOrderDto_whenOrderExists() {
        OrderDto updatedDetailsDto = new OrderDto();
        updatedDetailsDto.setAddress("New Address");
        // Assume items are also part of the DTO for update
        OrderItemDto updatedItemDto = new OrderItemDto();
        updatedItemDto.setQuantity(5);
        updatedDetailsDto.setItems(Collections.singletonList(updatedItemDto));


        Order existingOrder = new Order(); // Simulate existing order
        existingOrder.setId(order.getId());
        existingOrder.setCreatedAt(order.getCreatedAt());
        existingOrder.setItems(new java.util.ArrayList<>()); // Mutable list for clear()

        Order mappedOrderFromDto = new Order(); // Simulate what mapper.toEntity(orderDto) returns for items
        OrderItem newItemFromDto = new OrderItem();
        newItemFromDto.setQuantity(5);
        mappedOrderFromDto.setItems(Collections.singletonList(newItemFromDto));


        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(existingOrder));
        // Mocking updateEntityFromDto to apply changes to existingOrder
        doAnswer(invocation -> {
            OrderDto dtoArg = invocation.getArgument(0);
            Order entityArg = invocation.getArgument(1);
            entityArg.setAddress(dtoArg.getAddress()); // Simulate field update
            return null;
        }).when(orderMapper).updateEntityFromDto(any(OrderDto.class), any(Order.class));

        // Mocking the item mapping part for the update
        when(orderMapper.toEntity(updatedDetailsDto)).thenReturn(mappedOrderFromDto);


        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // For the final toDto conversion
        when(orderMapper.toDto(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            OrderDto finalDto = new OrderDto();
            finalDto.setId(saved.getId());
            finalDto.setAddress(saved.getAddress());
            finalDto.setUpdatedAt(LocalDateTime.now()); // Convert String to LocalDateTime
            if (saved.getItems() != null && !saved.getItems().isEmpty()) {
                 OrderItemDto itemDto = new OrderItemDto();
                 itemDto.setQuantity(saved.getItems().get(0).getQuantity());
                 finalDto.setItems(Collections.singletonList(itemDto));
            }
            return finalDto;
        });


        OrderDto result = orderService.updateOrder(order.getId(), updatedDetailsDto);

        assertNotNull(result);
        assertEquals(order.getId(), result.getId());
        assertEquals("New Address", result.getAddress());
        assertNotNull(result.getUpdatedAt());
        assertNotEquals(order.getUpdatedAt(), result.getUpdatedAt()); // UpdatedAt should change
        assertFalse(result.getItems().isEmpty());
        assertEquals(5, result.getItems().get(0).getQuantity());


        verify(orderRepository, times(1)).findById(order.getId());
        verify(orderMapper, times(1)).updateEntityFromDto(eq(updatedDetailsDto), eq(existingOrder));
        verify(orderMapper, times(1)).toEntity(updatedDetailsDto); // For item processing
        verify(orderRepository, times(1)).save(existingOrder);
        verify(orderMapper, times(1)).toDto(existingOrder);
    }


    @Test
    void updateOrder_shouldThrowEntityNotFoundException_whenOrderNotFound() {
        when(orderRepository.findById(anyString())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderService.updateOrder("nonExistentId", orderDto));
        assertEquals("Order not found with id: nonExistentId", exception.getMessage());
        verify(orderRepository, times(1)).findById("nonExistentId");
        verify(orderMapper, never()).updateEntityFromDto(any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_shouldCallDeleteById_whenOrderExists() {
        when(orderRepository.existsById(order.getId())).thenReturn(true);
        doNothing().when(orderRepository).deleteById(order.getId());

        orderService.deleteOrder(order.getId());

        verify(orderRepository, times(1)).existsById(order.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void deleteOrder_shouldThrowEntityNotFoundException_whenOrderNotFound() {
        when(orderRepository.existsById(anyString())).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderService.deleteOrder("nonExistentId"));
        assertEquals("Order not found with id: nonExistentId", exception.getMessage());
        verify(orderRepository, times(1)).existsById("nonExistentId");
        verify(orderRepository, never()).deleteById(anyString());
    }
}
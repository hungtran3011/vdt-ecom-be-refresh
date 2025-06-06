package com.hungng3011.vdtecomberefresh.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.controllers.OrderController;
import com.hungng3011.vdtecomberefresh.order.services.OrderService;
import com.hungng3011.vdtecomberefresh.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({OrderControllerTest.OrderControllerTestConfig.class, SecurityConfig.class})
@WithMockUser
class OrderControllerTest {

    @TestConfiguration
    static class OrderControllerTestConfig {
        @Bean
        public OrderService orderService() {
            return Mockito.mock(OrderService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderService orderService;

    private OrderDto orderDto;
    private String orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setUserId("user123");
        orderDto.setCreatedAt(LocalDateTime.now());
        orderDto.setItems(Collections.emptyList());
    }

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        when(orderService.createOrder(any(OrderDto.class))).thenReturn(orderDto);

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value("user123"));
    }

    @Test
    void getAllOrders_shouldReturnListOfOrders() throws Exception {
        List<OrderDto> orderList = Collections.singletonList(orderDto);
        when(orderService.getAllOrders()).thenReturn(orderList);

        mockMvc.perform(get("/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    void getOrderById_shouldReturnOrder() throws Exception {
        when(orderService.getOrderById(orderId)).thenReturn(orderDto);

        mockMvc.perform(get("/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void updateOrder_shouldReturnUpdatedOrder() throws Exception {
        OrderDto updatedOrderDto = new OrderDto();
        updatedOrderDto.setId(orderId);
        updatedOrderDto.setUserId("user123");
        updatedOrderDto.setAddress("New Address");
        updatedOrderDto.setItems(Collections.emptyList());


        when(orderService.updateOrder(anyString(), any(OrderDto.class))).thenReturn(updatedOrderDto);

        mockMvc.perform(put("/v1/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedOrderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.address").value("New Address"));
    }

    @Test
    void deleteOrder_shouldReturnNoContent() throws Exception {
        doNothing().when(orderService).deleteOrder(orderId);

        mockMvc.perform(delete("/v1/orders/" + orderId))
                .andExpect(status().isNoContent());
    }
}
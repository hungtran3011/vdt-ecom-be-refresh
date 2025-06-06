package com.hungng3011.vdtecomberefresh.cart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.services.CartService;
import com.hungng3011.vdtecomberefresh.config.SecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({CartControllerTest.CartControllerTestConfig.class, SecurityConfig.class})
@WithMockUser
class CartControllerTest {

    @TestConfiguration
    static class CartControllerTestConfig {
        @Bean
        public CartService cartService() {
            return Mockito.mock(CartService.class);
        }
    }

    @Autowired
    private CartService cartService;
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        cartDto = new CartDto();
        cartDto.setId(1L);
        cartDto.setUserId(100L);
    }

    @Test
    void createCart_shouldReturnCreatedCart() throws Exception {
        when(cartService.create(any(CartDto.class))).thenReturn(cartDto);

        mockMvc.perform(post("/v1/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartDto.getId()))
                .andExpect(jsonPath("$.userId").value(cartDto.getUserId()));
    }

    @Test
    void getCart_shouldReturnCartDto() throws Exception {
        when(cartService.get(1L)).thenReturn(cartDto);

        mockMvc.perform(get("/v1/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartDto.getId()))
                .andExpect(jsonPath("$.userId").value(cartDto.getUserId()));
    }

    @Test
    void getCart_shouldReturnNotFound_whenServiceThrowsException() throws Exception {
        when(cartService.get(1L)).thenThrow(new RuntimeException("Cart not found"));

        mockMvc.perform(get("/v1/cart/1"))
                .andExpect(status().isInternalServerError()); // Or map to a specific HTTP status in an @ControllerAdvice
    }


    @Test
    void updateCart_shouldReturnUpdatedCartDto() throws Exception {
        CartDto updatedCartDto = new CartDto();
        updatedCartDto.setId(1L);
        updatedCartDto.setUserId(101L);

        when(cartService.update(eq(1L), any(CartDto.class))).thenReturn(updatedCartDto);

        mockMvc.perform(put("/v1/cart/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCartDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedCartDto.getId()))
                .andExpect(jsonPath("$.userId").value(updatedCartDto.getUserId()));
    }

    @Test
    void deleteCart_shouldReturnNoContent() throws Exception {
        doNothing().when(cartService).delete(1L);

        mockMvc.perform(delete("/v1/cart/1"))
                .andExpect(status().isNoContent());
    }
}
package com.hungng3011.vdtecomberefresh.cart.services;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import com.hungng3011.vdtecomberefresh.cart.mappers.CartMapper;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(100L);

        cartDto = new CartDto();
        cartDto.setId(1L);
        cartDto.setUserId(100L);
    }

    @Test
    void create_shouldSaveAndReturnCartDto() {
        when(cartMapper.toEntity(any(CartDto.class))).thenReturn(cart);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toDto(any(Cart.class))).thenReturn(cartDto);

        CartDto result = cartService.create(cartDto);

        assertNotNull(result);
        assertEquals(cartDto.getId(), result.getId());
        verify(cartRepository, times(1)).save(cart);
        verify(cartMapper, times(1)).toEntity(cartDto);
        verify(cartMapper, times(1)).toDto(cart);
    }

    @Test
    void get_shouldReturnCartDto_whenCartExists() {
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(any(Cart.class))).thenReturn(cartDto);

        CartDto result = cartService.get(1L);

        assertNotNull(result);
        assertEquals(cartDto.getId(), result.getId());
        verify(cartRepository, times(1)).findById(1L);
        verify(cartMapper, times(1)).toDto(cart);
    }

    @Test
    void get_shouldThrowRuntimeException_whenCartNotFound() {
        when(cartRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cartService.get(1L));
        assertEquals("Cart processing failed for cart 1: Cart not found", exception.getMessage());
        verify(cartRepository, times(1)).findById(1L);
        verify(cartMapper, never()).toDto(any(Cart.class));
    }

    @Test
    void update_shouldUpdateAndReturnCartDto_whenCartExists() {
        CartDto updatedCartDto = new CartDto();
        updatedCartDto.setId(1L);
        updatedCartDto.setUserId(101L); // Different user ID

        Cart updatedCartEntity = new Cart();
        updatedCartEntity.setId(1L);
        updatedCartEntity.setUserId(101L);


        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toEntity(any(CartDto.class))).thenReturn(updatedCartEntity);
        when(cartRepository.save(any(Cart.class))).thenReturn(updatedCartEntity);
        when(cartMapper.toDto(any(Cart.class))).thenReturn(updatedCartDto);

        CartDto result = cartService.update(1L, updatedCartDto);

        assertNotNull(result);
        assertEquals(updatedCartDto.getUserId(), result.getUserId());
        verify(cartRepository, times(1)).findById(1L);
        verify(cartMapper, times(1)).toEntity(updatedCartDto);
        verify(cartRepository, times(1)).save(updatedCartEntity);
        verify(cartMapper, times(1)).toDto(updatedCartEntity);
    }

    @Test
    void update_shouldThrowRuntimeException_whenCartNotFound() {
        when(cartRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cartService.update(1L, cartDto));
        assertEquals("Cart processing failed for cart 1: Cart not found", exception.getMessage());
        verify(cartRepository, times(1)).findById(1L);
        verify(cartMapper, never()).toEntity(any(CartDto.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void delete_shouldCallDeleteById() {
        doNothing().when(cartRepository).deleteById(1L);

        cartService.delete(1L);

        verify(cartRepository, times(1)).deleteById(1L);
    }
}
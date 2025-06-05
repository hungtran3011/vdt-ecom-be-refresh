package com.hungng3011.vdtecomberefresh.cart.services;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.mappers.CartMapper;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartRepository;
import com.hungng3011.vdtecomberefresh.exception.cart.CartProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    public CartDto create(CartDto cartDto) {
        var cart = cartMapper.toEntity(cartDto);
        var saved = cartRepository.save(cart);
        return cartMapper.toDto(saved);
    }

    public CartDto get(Long id) {
        var cart = cartRepository.findById(id)
                .orElseThrow(() -> new CartProcessingException("Cart not found", id));
        return cartMapper.toDto(cart);
    }

    public CartDto update(Long id, CartDto cartDto) {
        var existing = cartRepository.findById(id)
                .orElseThrow(() -> new CartProcessingException("Cart not found", id));
        var updated = cartMapper.toEntity(cartDto);
        updated.setId(existing.getId());
        var saved = cartRepository.save(updated);
        return cartMapper.toDto(saved);
    }

    public void delete(Long id) {
        cartRepository.deleteById(id);
    }
}

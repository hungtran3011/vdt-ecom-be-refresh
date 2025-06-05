package com.hungng3011.vdtecomberefresh.cart.controllers;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.services.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDto> createCart(@Valid @RequestBody CartDto cartDto) {
        CartDto createdCart = cartService.create(cartDto);
        return new ResponseEntity<>(createdCart, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto> getCart(@PathVariable Long id) {
        CartDto cartDto = cartService.get(id);
        return ResponseEntity.ok(cartDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto> updateCart(@PathVariable Long id, @RequestBody CartDto cartDto) {
        CartDto updatedCart = cartService.update(id, cartDto);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        cartService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
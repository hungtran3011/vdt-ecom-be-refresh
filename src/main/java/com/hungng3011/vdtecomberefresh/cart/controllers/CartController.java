package com.hungng3011.vdtecomberefresh.cart.controllers;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.services.CartService;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDto> createCart(@Valid @RequestBody CartDto cartDto) {
        log.info("Creating new cart for user: {}", cartDto.getUserId() != null ? cartDto.getUserId() : "guest");
        try {
            CartDto createdCart = cartService.create(cartDto);
            log.info("Successfully created cart with ID: {}", createdCart.getId());
            return new ResponseEntity<>(createdCart, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating cart for user: {}", cartDto.getUserId() != null ? cartDto.getUserId() : "guest", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto> getCart(@PathVariable Long id) {
        log.info("Fetching cart with ID: {}", id);
        try {
            CartDto cartDto = cartService.get(id);
            log.info("Successfully retrieved cart with ID: {}", id);
            return ResponseEntity.ok(cartDto);
        } catch (Exception e) {
            log.error("Error fetching cart with ID: {}", id, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto> updateCart(@PathVariable Long id, @RequestBody CartDto cartDto) {
        log.info("Updating cart with ID: {}", id);
        try {
            CartDto updatedCart = cartService.update(id, cartDto);
            log.info("Successfully updated cart with ID: {}", id);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            log.error("Error updating cart with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        log.info("Deleting cart with ID: {}", id);
        try {
            cartService.delete(id);
            log.info("Successfully deleted cart with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting cart with ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Get cart items for a specific cart with cursor-based pagination
     * @param cartId Cart ID to get items for
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param cursor Optional cursor for pagination
     * @return PagedResponse containing cart items
     */
    @GetMapping("/{cartId}/items")
    public ResponseEntity<PagedResponse<CartItemDto>> getCartItems(
            @PathVariable Long cartId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        log.info("Fetching cart items for cart: {} with pagination - page: {}, size: {}, cursor: {}", 
                cartId, page, size, cursor);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            PagedResponse<CartItemDto> response = cartService.getCartItemsWithPagination(cartId, pageable, cursor);
            
            log.info("Successfully retrieved {} cart items for cart: {} (page: {}, total: {})", 
                    response.getContent().size(), cartId, page, response.getPagination().getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching cart items for cart: {} with pagination", cartId, e);
            throw e;
        }
    }

    /**
     * Get previous page of cart items for a specific cart
     * @param cartId Cart ID to get items for
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param cursor Cursor for pagination (required for previous page)
     * @return PagedResponse containing cart items
     */
    @GetMapping("/{cartId}/items/previous")
    public ResponseEntity<PagedResponse<CartItemDto>> getPreviousCartItems(
            @PathVariable Long cartId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String cursor) {
        
        log.info("Fetching previous cart items for cart: {} with cursor: {}", cartId, cursor);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            PagedResponse<CartItemDto> response = cartService.getCartItemsWithPreviousCursor(cartId, pageable, cursor);
            
            log.info("Successfully retrieved {} previous cart items for cart: {}", 
                    response.getContent().size(), cartId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching previous cart items for cart: {} with cursor: {}", cartId, cursor, e);
            throw e;
        }
    }

    /**
     * Get all cart items for a specific user with cursor-based pagination
     * @param userId User ID to filter by
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param cursor Optional cursor for pagination
     * @return PagedResponse containing cart items
     */
    @GetMapping("/user/{userId}/items")
    public ResponseEntity<PagedResponse<CartItemDto>> getUserCartItems(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        log.info("Fetching cart items for user: {} with pagination - page: {}, size: {}, cursor: {}", 
                userId, page, size, cursor);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            PagedResponse<CartItemDto> response = cartService.getCartItemsByUserWithPagination(userId, pageable, cursor);
            
            log.info("Successfully retrieved {} cart items for user: {} (page: {}, total: {})", 
                    response.getContent().size(), userId, page, response.getPagination().getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching cart items for user: {} with pagination", userId, e);
            throw e;
        }
    }

    /**
     * Get previous page of cart items for a specific user
     * @param userId User ID to filter by
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param cursor Cursor for pagination (required for previous page)
     * @return PagedResponse containing cart items
     */
    @GetMapping("/user/{userId}/items/previous")
    public ResponseEntity<PagedResponse<CartItemDto>> getPreviousUserCartItems(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String cursor) {
        
        log.info("Fetching previous cart items for user: {} with cursor: {}", userId, cursor);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            PagedResponse<CartItemDto> response = cartService.getCartItemsByUserWithPreviousCursor(userId, pageable, cursor);
            
            log.info("Successfully retrieved {} previous cart items for user: {}", 
                    response.getContent().size(), userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching previous cart items for user: {} with cursor: {}", userId, cursor, e);
            throw e;
        }
    }
}
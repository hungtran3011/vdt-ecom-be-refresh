package com.hungng3011.vdtecomberefresh.cart.controllers;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.dtos.AddToCartRequest;
import com.hungng3011.vdtecomberefresh.cart.dtos.UpdateCartItemRequest;
import com.hungng3011.vdtecomberefresh.cart.services.CartItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Cart Item operations with stock validation
 */
@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartItemController {
    
    private final CartItemService cartItemService;

    /**
     * Add item to cart with stock validation
     * @param cartId Cart ID to add item to
     * @param request Add to cart request containing product details
     * @return CartItemDto representing the added item
     */
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartItemDto> addItemToCart(
            @PathVariable Long cartId,
            @Valid @RequestBody AddToCartRequest request) {
        log.info("Adding item to cart {} - productId: {}, quantity: {}", 
                cartId, request.getProductId(), request.getQuantity());
        try {
            CartItemDto cartItem = cartItemService.addItemToCart(cartId, request);
            log.info("Successfully added item to cart {} - item ID: {}", cartId, cartItem.getId());
            return new ResponseEntity<>(cartItem, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error adding item to cart {} - productId: {}", cartId, request.getProductId(), e);
            throw e;
        }
    }

    /**
     * Update cart item quantity with stock validation
     * @param cartId Cart ID
     * @param itemId Cart item ID to update
     * @param request Update request containing new quantity
     * @return Updated CartItemDto
     */
    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartItemDto> updateCartItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Updating cart item {} in cart {} - new quantity: {}", 
                itemId, cartId, request.getQuantity());
        try {
            CartItemDto updatedItem = cartItemService.updateCartItem(cartId, itemId, request);
            log.info("Successfully updated cart item {} in cart {}", itemId, cartId);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            log.error("Error updating cart item {} in cart {}", itemId, cartId, e);
            throw e;
        }
    }

    /**
     * Remove item from cart
     * @param cartId Cart ID
     * @param itemId Cart item ID to remove
     * @return No content response
     */
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId) {
        log.info("Removing cart item {} from cart {}", itemId, cartId);
        try {
            cartItemService.removeCartItem(cartId, itemId);
            log.info("Successfully removed cart item {} from cart {}", itemId, cartId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error removing cart item {} from cart {}", itemId, cartId, e);
            throw e;
        }
    }

    /**
     * Get specific cart item
     * @param cartId Cart ID
     * @param itemId Cart item ID
     * @return CartItemDto
     */
    @GetMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartItemDto> getCartItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId) {
        log.info("Fetching cart item {} from cart {}", itemId, cartId);
        try {
            CartItemDto cartItem = cartItemService.getCartItem(cartId, itemId);
            if (cartItem == null) {
                log.warn("Cart item {} not found in cart {}", itemId, cartId);
                return ResponseEntity.notFound().build();
            }
            log.info("Successfully retrieved cart item {} from cart {}", itemId, cartId);
            return ResponseEntity.ok(cartItem);
        } catch (Exception e) {
            log.error("Error fetching cart item {} from cart {}", itemId, cartId, e);
            throw e;
        }
    }
}

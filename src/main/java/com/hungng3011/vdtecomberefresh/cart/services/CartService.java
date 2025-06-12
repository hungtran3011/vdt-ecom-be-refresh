package com.hungng3011.vdtecomberefresh.cart.services;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.entities.CartItem;
import com.hungng3011.vdtecomberefresh.cart.mappers.CartMapper;
import com.hungng3011.vdtecomberefresh.cart.mappers.CartItemMapper;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartRepository;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartItemRepository;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.exception.cart.CartProcessingException;
import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;

    public CartDto create(CartDto cartDto) {
        log.info("Creating new cart for user: {}", cartDto.getUserId() != null ? cartDto.getUserId() : "guest");
        try {
            var cart = cartMapper.toEntity(cartDto);
            var saved = cartRepository.save(cart);
            log.info("Successfully created cart with ID: {}", saved.getId());
            return cartMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating cart for user: {}", cartDto.getUserId() != null ? cartDto.getUserId() : "guest", e);
            throw e;
        }
    }

    public CartDto get(Long id) {
        log.info("Fetching cart with ID: {}", id);
        try {
            var cart = cartRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Cart not found with ID: {}", id);
                        return new CartProcessingException("Cart not found", id);
                    });
            log.info("Successfully retrieved cart with ID: {}", id);
            return cartMapper.toDto(cart);
        } catch (CartProcessingException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error fetching cart with ID: {}", id, e);
            throw e;
        }
    }

    public CartDto update(Long id, CartDto cartDto) {
        log.info("Updating cart with ID: {}", id);
        try {
            var existing = cartRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Cart not found for update with ID: {}", id);
                        return new CartProcessingException("Cart not found", id);
                    });
            var updated = cartMapper.toEntity(cartDto);
            updated.setId(existing.getId());
            var saved = cartRepository.save(updated);
            log.info("Successfully updated cart with ID: {}", id);
            return cartMapper.toDto(saved);
        } catch (CartProcessingException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error updating cart with ID: {}", id, e);
            throw e;
        }
    }

    public void delete(Long id) {
        log.info("Deleting cart with ID: {}", id);
        try {
            cartRepository.deleteById(id);
            log.info("Successfully deleted cart with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting cart with ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Get cart items for a specific cart with cursor-based pagination
     * @param cartId Cart ID to get items for
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing cart items and pagination metadata
     */
    public PagedResponse<CartItemDto> getCartItemsWithPagination(Long cartId, Pageable pageable, String cursor) {
        log.info("Finding cart items for cart: {} with pagination - page: {}, size: {}, cursor: {}", 
                cartId, pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<CartItem> cartItems;
        
        if (cursorLong != null) {
            // Use cursor-based pagination
            cartItems = cartItemRepository.findByCartIdWithCursorAfter(cartId, cursorLong, pageable);
        } else {
            // First page - use standard pagination
            cartItems = cartItemRepository.findByCartIdWithCursorAfter(cartId, null, pageable);
        }

        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());

        // Get total count for pagination metadata
        long totalElements = cartItemRepository.countByCartId(cartId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        // Calculate cursors
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!cartItems.isEmpty()) {
            nextCursor = cartItems.get(cartItems.size() - 1).getId();
            if (cursor != null) {
                previousCursor = Long.parseLong(cursor);
            }
        }

        // Build pagination metadata
        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(cartItems.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} cart items for cart: {} (page: {}, total: {})", 
                cartItemDtos.size(), cartId, pageable.getPageNumber(), totalElements);

        return PagedResponse.<CartItemDto>builder()
                .content(cartItemDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get cart items for a specific cart with cursor-based pagination (previous page)
     * @param cartId Cart ID to get items for
     * @param pageable Pageable information
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing cart items and pagination metadata
     */
    public PagedResponse<CartItemDto> getCartItemsWithPreviousCursor(Long cartId, Pageable pageable, String cursor) {
        log.info("Finding previous cart items for cart: {} with cursor: {}", cartId, cursor);
        
        Long cursorLong = Long.parseLong(cursor);
        List<CartItem> cartItems = cartItemRepository.findByCartIdWithCursorBefore(cartId, cursorLong, pageable);
        
        // Reverse the order since we queried in DESC order
        java.util.Collections.reverse(cartItems);

        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = cartItemRepository.countByCartId(cartId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = Long.parseLong(cursor);
        Object previousCursor = !cartItems.isEmpty() ? cartItems.get(0).getId() : null;

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(Math.max(0, pageable.getPageNumber() - 1))
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(true)
                .hasPrevious(cartItems.size() == pageable.getPageSize())
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        return PagedResponse.<CartItemDto>builder()
                .content(cartItemDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get cart items by user ID with cursor-based pagination
     * @param userId User ID to filter by
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing cart items and pagination metadata
     */
    public PagedResponse<CartItemDto> getCartItemsByUserWithPagination(Long userId, Pageable pageable, String cursor) {
        log.info("Finding cart items for user: {} with pagination - page: {}, size: {}, cursor: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<CartItem> cartItems;
        
        if (cursorLong != null) {
            cartItems = cartItemRepository.findByUserIdWithCursorAfter(userId, cursorLong, pageable);
        } else {
            cartItems = cartItemRepository.findByUserIdWithCursorAfter(userId, null, pageable);
        }

        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = cartItemRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!cartItems.isEmpty()) {
            nextCursor = cartItems.get(cartItems.size() - 1).getId();
            if (cursor != null) {
                previousCursor = Long.parseLong(cursor);
            }
        }

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(cartItems.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} cart items for user: {} (page: {}, total: {})", 
                cartItemDtos.size(), userId, pageable.getPageNumber(), totalElements);

        return PagedResponse.<CartItemDto>builder()
                .content(cartItemDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get cart items by user ID with cursor-based pagination (previous page)
     * @param userId User ID to filter by
     * @param pageable Pageable information
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing cart items and pagination metadata
     */
    public PagedResponse<CartItemDto> getCartItemsByUserWithPreviousCursor(Long userId, Pageable pageable, String cursor) {
        log.info("Finding previous cart items for user: {} with cursor: {}", userId, cursor);
        
        Long cursorLong = Long.parseLong(cursor);
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithCursorBefore(userId, cursorLong, pageable);
        
        // Reverse the order since we queried in DESC order
        java.util.Collections.reverse(cartItems);

        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = cartItemRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = Long.parseLong(cursor);
        Object previousCursor = !cartItems.isEmpty() ? cartItems.get(0).getId() : null;

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(Math.max(0, pageable.getPageNumber() - 1))
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(true)
                .hasPrevious(cartItems.size() == pageable.getPageSize())
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        return PagedResponse.<CartItemDto>builder()
                .content(cartItemDtos)
                .pagination(pagination)
                .build();
    }

    // Email-based cart methods for Keycloak integration

    /**
     * Get cart by user email
     * @param userEmail User email from JWT
     * @return CartDto for the user
     */
    public CartDto getCartByEmail(String userEmail) {
        log.info("Fetching cart for user email: {}", userEmail);
        try {
            return getOrCreateCartByEmail(userEmail);
        } catch (Exception e) {
            log.error("Error fetching cart for user email: {}", userEmail, e);
            throw e;
        }
    }

    /**
     * Get or create cart by user email
     * @param userEmail User email from JWT
     * @return CartDto for the user
     */
    public CartDto getOrCreateCartByEmail(String userEmail) {
        log.info("Getting or creating cart for user email: {}", userEmail);
        try {
            // Try to find existing cart by email
            var existingCart = cartRepository.findActiveCartByUserEmail(userEmail);
            
            if (existingCart.isPresent()) {
                log.info("Found existing cart for user email: {}", userEmail);
                return cartMapper.toDto(existingCart.get());
            }
            
            // Create new cart if none exists
            log.info("Creating new cart for user email: {}", userEmail);
            CartDto cartDto = new CartDto();
            cartDto.setUserEmail(userEmail);
            return create(cartDto);
        } catch (Exception e) {
            log.error("Error getting or creating cart for user email: {}", userEmail, e);
            throw e;
        }
    }

    /**
     * Get cart items by user email with pagination
     * @param userEmail User email from JWT
     * @param page Page number
     * @param size Page size  
     * @param cursor Optional cursor for pagination
     * @return PagedResponse containing cart items
     */
    public PagedResponse<CartItemDto> getCartItemsByEmailWithPagination(String userEmail, int page, int size, String cursor) {
        log.info("Finding cart items for user email: {} with pagination - page: {}, size: {}, cursor: {}", 
                userEmail, page, size, cursor);
        try {
            // Get cart for user
            CartDto cart = getOrCreateCartByEmail(userEmail);
            
            Pageable pageable = PageRequest.of(page, size);
            return getCartItemsWithPagination(cart.getId(), pageable, cursor);
        } catch (Exception e) {
            log.error("Error finding cart items for user email: {} with pagination", userEmail, e);
            throw e;
        }
    }

    /**
     * Add item to cart by user email
     * @param userEmail User email from JWT
     * @param cartItemDto Cart item to add
     * @return Updated cart
     */
    public CartDto addItemToCartByEmail(String userEmail, CartItemDto cartItemDto) {
        log.info("Adding item to cart for user email: {}", userEmail);
        try {
            // Get or create cart for user
            CartDto cart = getOrCreateCartByEmail(userEmail);
            
            // Add item to cart (assuming there's an existing method)
            return addItemToCart(cart.getId(), cartItemDto);
        } catch (Exception e) {
            log.error("Error adding item to cart for user email: {}", userEmail, e);
            throw e;
        }
    }

    /**
     * Update cart item by user email
     * @param userEmail User email from JWT
     * @param itemId Cart item ID
     * @param cartItemDto Updated cart item
     * @return Updated cart
     */
    public CartDto updateCartItemByEmail(String userEmail, Long itemId, CartItemDto cartItemDto) {
        log.info("Updating cart item {} for user email: {}", itemId, userEmail);
        try {
            // Get cart for user
            CartDto cart = getOrCreateCartByEmail(userEmail);
            
            // Update item in cart (assuming there's an existing method)
            return updateCartItem(cart.getId(), itemId, cartItemDto);
        } catch (Exception e) {
            log.error("Error updating cart item {} for user email: {}", itemId, userEmail, e);
            throw e;
        }
    }

    /**
     * Remove item from cart by user email
     * @param userEmail User email from JWT
     * @param itemId Cart item ID
     * @return Updated cart
     */
    public CartDto removeItemFromCartByEmail(String userEmail, Long itemId) {
        log.info("Removing cart item {} for user email: {}", itemId, userEmail);
        try {
            // Get cart for user
            CartDto cart = getOrCreateCartByEmail(userEmail);
            
            // Remove item from cart (assuming there's an existing method)
            return removeItemFromCart(cart.getId(), itemId);
        } catch (Exception e) {
            log.error("Error removing cart item {} for user email: {}", itemId, userEmail, e);
            throw e;
        }
    }

    /**
     * Clear all items from cart by user email
     * @param userEmail User email from JWT
     * @return Empty cart
     */
    public CartDto clearCartByEmail(String userEmail) {
        log.info("Clearing cart for user email: {}", userEmail);
        try {
            // Get cart for user
            CartDto cart = getOrCreateCartByEmail(userEmail);
            
            // Clear all items from cart (assuming there's an existing method)
            return clearCart(cart.getId());
        } catch (Exception e) {
            log.error("Error clearing cart for user email: {}", userEmail, e);
            throw e;
        }
    }

    // Helper methods (these would need to be implemented if they don't exist)
    
    private CartDto addItemToCart(Long cartId, CartItemDto cartItemDto) {
        log.info("Adding item to cart {}: product {}, quantity {}", 
                cartId, cartItemDto.getProductId(), cartItemDto.getQuantity());
        
        try {
            // Get the cart
            var cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new CartProcessingException("Cart not found with ID: " + cartId));
            
            // Check if item already exists in cart (by product ID and selected variations)
            var existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(cartItemDto.getProductId()) 
                            && variationsMatch(item.getSelectedVariations(), cartItemDto.getSelectedVariations()))
                    .findFirst();
            
            if (existingItem.isPresent()) {
                // Update quantity if item exists
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + cartItemDto.getQuantity());
                cartItemRepository.save(item);
                log.info("Updated existing cart item quantity to {}", item.getQuantity());
            } else {
                // Add new item to cart
                CartItem newItem = cartItemMapper.toEntity(cartItemDto);
                newItem.setCart(cart);
                cartItemRepository.save(newItem);
                cart.getItems().add(newItem);
                log.info("Added new item to cart");
            }
            
            cartRepository.save(cart);
            return cartMapper.toDto(cart);
        } catch (Exception e) {
            log.error("Error adding item to cart {}", cartId, e);
            throw new CartProcessingException("Failed to add item to cart", e);
        }
    }

    private boolean variationsMatch(List<Variation> itemVariations, List<VariationDto> dtoVariations) {
        if (itemVariations.size() != dtoVariations.size()) {
            return false;
        }
        
        Set<Long> itemVariationIds = itemVariations.stream()
                .map(Variation::getId)
                .collect(Collectors.toSet());
        
        Set<Long> dtoVariationIds = dtoVariations.stream()
                .map(VariationDto::getId)
                .collect(Collectors.toSet());
        
        return itemVariationIds.equals(dtoVariationIds);
    }

    private CartDto updateCartItem(Long cartId, Long itemId, CartItemDto cartItemDto) {
        log.info("Updating cart item {} in cart {} with quantity {}", itemId, cartId, cartItemDto.getQuantity());
        
        try {
            // Get the cart item
            var cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new CartProcessingException("Cart item not found with ID: " + itemId));
            
            // Verify the item belongs to the correct cart
            if (!cartItem.getCart().getId().equals(cartId)) {
                throw new CartProcessingException("Cart item " + itemId + " does not belong to cart " + cartId);
            }
            
            // Update the item
            cartItem.setQuantity(cartItemDto.getQuantity());
            if (cartItemDto.getUnitPrice() != null) {
                cartItem.setUnitPrice(cartItemDto.getUnitPrice());
            }
            
            cartItemRepository.save(cartItem);
            
            // Return updated cart
            return get(cartId);
        } catch (Exception e) {
            log.error("Error updating cart item {} in cart {}", itemId, cartId, e);
            throw new CartProcessingException("Failed to update cart item", e);
        }
    }

    private CartDto removeItemFromCart(Long cartId, Long itemId) {
        log.info("Removing cart item {} from cart {}", itemId, cartId);
        
        try {
            // Get the cart item
            var cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new CartProcessingException("Cart item not found with ID: " + itemId));
            
            // Verify the item belongs to the correct cart
            if (!cartItem.getCart().getId().equals(cartId)) {
                throw new CartProcessingException("Cart item " + itemId + " does not belong to cart " + cartId);
            }
            
            // Remove the item from cart
            var cart = cartItem.getCart();
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
            cartRepository.save(cart);
            
            log.info("Successfully removed cart item {} from cart {}", itemId, cartId);
            return cartMapper.toDto(cart);
        } catch (Exception e) {
            log.error("Error removing cart item {} from cart {}", itemId, cartId, e);
            throw new CartProcessingException("Failed to remove cart item", e);
        }
    }

    private CartDto clearCart(Long cartId) {
        log.info("Clearing all items from cart {}", cartId);
        
        try {
            // Get the cart
            var cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new CartProcessingException("Cart not found with ID: " + cartId));
            
            // Remove all items
            cart.getItems().clear();
            cartItemRepository.deleteByCartId(cartId);
            cartRepository.save(cart);
            
            log.info("Successfully cleared all items from cart {}", cartId);
            return cartMapper.toDto(cart);
        } catch (Exception e) {
            log.error("Error clearing cart {}", cartId, e);
            throw new CartProcessingException("Failed to clear cart", e);
        }
    }
}

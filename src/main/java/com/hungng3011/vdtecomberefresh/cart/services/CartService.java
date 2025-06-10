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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
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
}

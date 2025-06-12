package com.hungng3011.vdtecomberefresh.cart.services;

import com.hungng3011.vdtecomberefresh.cart.dtos.AddToCartRequest;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.dtos.UpdateCartItemRequest;
import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import com.hungng3011.vdtecomberefresh.cart.entities.CartItem;
import com.hungng3011.vdtecomberefresh.cart.mappers.CartItemMapper;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartRepository;
import com.hungng3011.vdtecomberefresh.cart.repositories.CartItemRepository;
import com.hungng3011.vdtecomberefresh.exception.cart.CartProcessingException;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.product.repositories.VariationRepository;
import com.hungng3011.vdtecomberefresh.stock.StockService;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Cart Item operations with integrated stock validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemService {
    
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final CartItemMapper cartItemMapper;
    private final StockService stockService;

    /**
     * Add item to cart with comprehensive stock validation
     * @param cartId Cart ID to add item to
     * @param request Add to cart request with product details
     * @return CartItemDto representing the added item
     */
    @Transactional
    public CartItemDto addItemToCart(Long cartId, AddToCartRequest request) {
        log.info("Adding item to cart {} - productId: {}, quantity: {}, variationIds: {}", 
                cartId, request.getProductId(), request.getQuantity(), request.getVariationIds());
        
        try {
            // Validate cart exists
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> {
                        log.warn("Cart not found with ID: {}", cartId);
                        return new CartProcessingException("Cart not found", cartId);
                    });

            // Validate product exists
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> {
                        log.warn("Product not found with ID: {}", request.getProductId());
                        return new IllegalArgumentException("Product not found with id: " + request.getProductId());
                    });

            // Validate stock availability
            boolean stockAvailable = validateStockAvailability(request.getProductId(), 
                    request.getVariationIds(), request.getQuantity());
            
            if (!stockAvailable) {
                String errorMsg = String.format("Insufficient stock for product: %s (ID: %d). Requested quantity: %d", 
                        product.getName(), request.getProductId(), request.getQuantity());
                log.warn(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Check if item already exists in cart
            Optional<CartItem> existingItem = findExistingCartItem(cart, request.getProductId(), request.getVariationIds());
            
            if (existingItem.isPresent()) {
                // Update existing item quantity
                CartItem cartItem = existingItem.get();
                int newQuantity = cartItem.getQuantity() + request.getQuantity();
                
                // Validate new total quantity
                boolean newQuantityAvailable = validateStockAvailability(request.getProductId(), 
                        request.getVariationIds(), newQuantity);
                
                if (!newQuantityAvailable) {
                    String errorMsg = String.format("Insufficient stock for total quantity. Available stock cannot support %d items", newQuantity);
                    log.warn(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                
                cartItem.setQuantity(newQuantity);
                // Note: subtotal is calculated automatically via @Transient getSubtotal() method
                
                CartItem savedItem = cartItemRepository.save(cartItem);
                log.info("Updated existing cart item {} to quantity {}", savedItem.getId(), newQuantity);
                return cartItemMapper.toDto(savedItem);
                
            } else {
                // Create new cart item
                CartItem cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setQuantity(request.getQuantity());
                cartItem.setAddedAt(LocalDateTime.now());
                
                // Set product price as unit price (you might want to get this from a price service)
                cartItem.setUnitPrice(product.getBasePrice());
                // Note: subtotal is calculated automatically via @Transient getSubtotal() method
                
                // Handle variations if provided
                if (request.getVariationIds() != null && !request.getVariationIds().isEmpty()) {
                    List<Variation> variations = variationRepository.findAllById(request.getVariationIds());
                    cartItem.setSelectedVariations(variations);
                    
                    // Set stock SKU if provided or find appropriate one
                    if (request.getStockSku() != null) {
                        cartItem.setStockSku(request.getStockSku());
                    } else {
                        // Find appropriate stock SKU for this variation combination
                        String stockSku = findStockSkuForVariations(request.getProductId(), request.getVariationIds());
                        cartItem.setStockSku(stockSku);
                    }
                }
                
                CartItem savedItem = cartItemRepository.save(cartItem);
                log.info("Created new cart item {} with quantity {}", savedItem.getId(), request.getQuantity());
                return cartItemMapper.toDto(savedItem);
            }
            
        } catch (CartProcessingException | IllegalArgumentException | IllegalStateException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error adding item to cart {} - productId: {}", cartId, request.getProductId(), e);
            throw new RuntimeException("Failed to add item to cart", e);
        }
    }

    /**
     * Update cart item quantity with stock validation
     * @param cartId Cart ID
     * @param itemId Cart item ID to update
     * @param request Update request containing new quantity
     * @return Updated CartItemDto
     */
    @Transactional
    public CartItemDto updateCartItem(Long cartId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item {} in cart {} - new quantity: {}", itemId, cartId, request.getQuantity());
        
        try {
            // Validate cart exists
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> {
                        log.warn("Cart not found with ID: {}", cartId);
                        return new CartProcessingException("Cart not found", cartId);
                    });

            // Find cart item
            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> {
                        log.warn("Cart item not found with ID: {}", itemId);
                        return new IllegalArgumentException("Cart item not found with id: " + itemId);
                    });

            // Validate cart item belongs to the specified cart
            if (!cartItem.getCart().getId().equals(cartId)) {
                log.warn("Cart item {} does not belong to cart {}", itemId, cartId);
                throw new IllegalArgumentException("Cart item does not belong to the specified cart");
            }

            // Get variation IDs from the cart item
            List<Long> variationIds = cartItem.getSelectedVariations().stream()
                    .map(Variation::getId)
                    .toList();

            // Validate stock availability for new quantity
            boolean stockAvailable = validateStockAvailability(cartItem.getProduct().getId(), 
                    variationIds, request.getQuantity());
            
            if (!stockAvailable) {
                String errorMsg = String.format("Insufficient stock for product: %s. Requested quantity: %d", 
                        cartItem.getProduct().getName(), request.getQuantity());
                log.warn(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Update cart item
            cartItem.setQuantity(request.getQuantity());
            // Note: subtotal is calculated automatically via @Transient getSubtotal() method
            
            CartItem savedItem = cartItemRepository.save(cartItem);
            log.info("Updated cart item {} to quantity {}", itemId, request.getQuantity());
            return cartItemMapper.toDto(savedItem);
            
        } catch (CartProcessingException | IllegalArgumentException | IllegalStateException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error updating cart item {} in cart {}", itemId, cartId, e);
            throw new RuntimeException("Failed to update cart item", e);
        }
    }

    /**
     * Remove item from cart
     * @param cartId Cart ID
     * @param itemId Cart item ID to remove
     */
    @Transactional
    public void removeCartItem(Long cartId, Long itemId) {
        log.info("Removing cart item {} from cart {}", itemId, cartId);
        
        try {
            // Find cart item and validate it belongs to the cart
            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> {
                        log.warn("Cart item not found with ID: {}", itemId);
                        return new IllegalArgumentException("Cart item not found with id: " + itemId);
                    });

            // Validate cart item belongs to the specified cart
            if (!cartItem.getCart().getId().equals(cartId)) {
                log.warn("Cart item {} does not belong to cart {}", itemId, cartId);
                throw new IllegalArgumentException("Cart item does not belong to the specified cart");
            }

            cartItemRepository.delete(cartItem);
            log.info("Successfully removed cart item {} from cart {}", itemId, cartId);
            
        } catch (CartProcessingException | IllegalArgumentException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error removing cart item {} from cart {}", itemId, cartId, e);
            throw new RuntimeException("Failed to remove cart item", e);
        }
    }

    /**
     * Get specific cart item
     * @param cartId Cart ID
     * @param itemId Cart item ID
     * @return CartItemDto or null if not found
     */
    public CartItemDto getCartItem(Long cartId, Long itemId) {
        log.info("Fetching cart item {} from cart {}", itemId, cartId);
        
        try {
            // Find cart item
            Optional<CartItem> cartItemOpt = cartItemRepository.findById(itemId);
            
            if (cartItemOpt.isEmpty()) {
                log.warn("Cart item not found with ID: {}", itemId);
                return null;
            }
            
            CartItem cartItem = cartItemOpt.get();
            
            // Validate cart item belongs to the specified cart
            if (!cartItem.getCart().getId().equals(cartId)) {
                log.warn("Cart item {} does not belong to cart {}", itemId, cartId);
                return null;
            }

            log.info("Successfully retrieved cart item {} from cart {}", itemId, cartId);
            return cartItemMapper.toDto(cartItem);
            
        } catch (Exception e) {
            log.error("Error fetching cart item {} from cart {}", itemId, cartId, e);
            throw new RuntimeException("Failed to fetch cart item", e);
        }
    }

    // Private helper methods

    /**
     * Validate stock availability using the stock service
     */
    private boolean validateStockAvailability(Long productId, List<Long> variationIds, Integer quantity) {
        try {
            if (variationIds == null || variationIds.isEmpty()) {
                // Simple product validation
                List<StockDto> stocks = stockService.getByProductId(productId);
                if (stocks.isEmpty()) {
                    return false;
                }
                // Check if any stock has sufficient quantity
                return stocks.stream().anyMatch(stock -> stock.getQuantity() >= quantity);
            } else {
                // Product with variations validation
                return stockService.validateStockForVariationCombination(productId, variationIds, quantity);
            }
        } catch (Exception e) {
            log.error("Error validating stock availability for product {} with variations {} and quantity {}", 
                    productId, variationIds, quantity, e);
            return false;
        }
    }

    /**
     * Find existing cart item with same product and variations
     */
    private Optional<CartItem> findExistingCartItem(Cart cart, Long productId, List<Long> variationIds) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> {
                    List<Long> itemVariationIds = item.getSelectedVariations().stream()
                            .map(Variation::getId)
                            .toList();
                    
                    if (variationIds == null || variationIds.isEmpty()) {
                        return itemVariationIds.isEmpty();
                    }
                    
                    return itemVariationIds.size() == variationIds.size() && 
                           itemVariationIds.containsAll(variationIds);
                })
                .findFirst();
    }

    /**
     * Find appropriate stock SKU for variation combination
     */
    private String findStockSkuForVariations(Long productId, List<Long> variationIds) {
        try {
            List<StockDto> stocks = stockService.getByProductAndVariations(productId, variationIds);
            if (!stocks.isEmpty()) {
                return stocks.get(0).getSku(); // Return first available stock SKU
            }
            return null;
        } catch (Exception e) {
            log.warn("Could not find stock SKU for product {} with variations {}", productId, variationIds, e);
            return null;
        }
    }
}

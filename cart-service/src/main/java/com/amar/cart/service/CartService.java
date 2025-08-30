package com.amar.cart.service;

import com.amar.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {
    
    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    
    private static final String CART_KEY_PREFIX_ANON = "cart:anon:";
    private static final String CART_KEY_PREFIX_AUTH = "cart:auth:";
    private static final int ANONYMOUS_CART_TTL_DAYS = 15;
    private static final int AUTHENTICATED_CART_TTL_DAYS = 30;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductValidationService productValidationService;
    
    // Get Cart
    public CartDto getCart(String userId, String sessionId) {
        String cartKey = getCartKey(userId, sessionId);
        log.debug("Getting cart for key: {}", cartKey);
        
        Map<Object, Object> cartData = redisTemplate.opsForHash().entries(cartKey);
        
        if (cartData.isEmpty()) {
            log.debug("Cart not found, creating empty cart");
            return createEmptyCart(userId, sessionId);
        }
        
        return buildCartFromRedisData(cartKey, cartData, userId, sessionId);
    }
    
    // Add Item to Cart
    @Transactional
    public CartDto addItemToCart(String userId, String sessionId, AddToCartRequest request) {
        log.info("Adding item to cart - User: {}, Session: {}, Product: {}, Quantity: {}", 
                userId, sessionId, request.getProductId(), request.getQuantity());
        
        // Validate product exists and get current price
        ProductDto product = productValidationService.validateProduct(request.getProductId());
        
        String cartKey = getCartKey(userId, sessionId);
        String productKey = "product:" + request.getProductId();
        
        // Check if item already exists in cart
        CartItemData existingItem = (CartItemData) redisTemplate.opsForHash().get(cartKey, productKey);
        
        int newQuantity = request.getQuantity();
        if (existingItem != null) {
            // Update quantity if item already exists
            newQuantity += existingItem.getQuantity();
            log.debug("Item exists in cart, updating quantity to: {}", newQuantity);
        }
        
        // Prepare cart item data
        CartItemData itemData = new CartItemData(
            newQuantity,
            product.getPrice(),
            Instant.now().toString(),
            product.getName(),
            product.getImageUrl()
        );
        
        // Store in Redis Hash atomically
        redisTemplate.opsForHash().put(cartKey, productKey, itemData);
        
        // Set TTL
        setCartTtl(cartKey, userId != null);
        
        log.info("Successfully added item to cart - Product: {}, Final Quantity: {}", 
                request.getProductId(), newQuantity);
        
        return getCart(userId, sessionId);
    }
    
    // Update Item Quantity
    public CartDto updateItemQuantity(String userId, String sessionId, UpdateCartItemRequest request) {
        log.info("Updating cart item quantity - User: {}, Product: {}, Quantity: {}", 
                userId, request.getProductId(), request.getQuantity());
        
        String cartKey = getCartKey(userId, sessionId);
        String productKey = "product:" + request.getProductId();
        
        // Check if item exists
        if (!redisTemplate.opsForHash().hasKey(cartKey, productKey)) {
            throw new CartItemNotFoundException("Item not found in cart: " + request.getProductId());
        }
        
        // Get existing item data
        CartItemData existingItem = (CartItemData) redisTemplate.opsForHash().get(cartKey, productKey);
        existingItem.setQuantity(request.getQuantity());
        
        // Update in Redis
        redisTemplate.opsForHash().put(cartKey, productKey, existingItem);
        
        log.info("Successfully updated item quantity - Product: {}, New Quantity: {}", 
                request.getProductId(), request.getQuantity());
        
        return getCart(userId, sessionId);
    }
    
    // Remove Item from Cart
    public CartDto removeItemFromCart(String userId, String sessionId, Long productId) {
        log.info("Removing item from cart - User: {}, Product: {}", userId, productId);
        
        String cartKey = getCartKey(userId, sessionId);
        String productKey = "product:" + productId;
        
        Long removedCount = redisTemplate.opsForHash().delete(cartKey, productKey);
        
        if (removedCount > 0) {
            log.info("Successfully removed item from cart - Product: {}", productId);
        } else {
            log.warn("Attempted to remove non-existent item from cart - Product: {}", productId);
        }
        
        return getCart(userId, sessionId);
    }
    
    // Clear Cart
    public void clearCart(String userId, String sessionId) {
        String cartKey = getCartKey(userId, sessionId);
        log.info("Clearing cart - Key: {}", cartKey);
        
        Boolean deleted = redisTemplate.delete(cartKey);
        log.info("Cart clear result - Key: {}, Deleted: {}", cartKey, deleted);
    }
    
    // Get cart item count
    public int getCartItemCount(String userId, String sessionId) {
        String cartKey = getCartKey(userId, sessionId);
        Map<Object, Object> cartData = redisTemplate.opsForHash().entries(cartKey);
        
        return cartData.values().stream()
            .mapToInt(item -> ((CartItemData) item).getQuantity())
            .sum();
    }
    
    // Helper Methods
    private String getCartKey(String userId, String sessionId) {
        return userId != null ? CART_KEY_PREFIX_AUTH + userId : CART_KEY_PREFIX_ANON + sessionId;
    }
    
    private void setCartTtl(String cartKey, boolean isAuthenticated) {
        int ttlDays = isAuthenticated ? AUTHENTICATED_CART_TTL_DAYS : ANONYMOUS_CART_TTL_DAYS;
        redisTemplate.expire(cartKey, Duration.ofDays(ttlDays));
        log.debug("Set TTL for cart key: {} - {} days", cartKey, ttlDays);
    }
    
    private CartDto createEmptyCart(String userId, String sessionId) {
        CartDto cart = new CartDto();
        cart.setCartId(UUID.randomUUID().toString());
        cart.setUserId(userId);
        cart.setSessionId(sessionId);
        cart.setItems(new ArrayList<>());
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
        return cart;
    }
    
    private CartDto buildCartFromRedisData(String cartKey, Map<Object, Object> cartData, String userId, String sessionId) {
        List<CartItemDto> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Map.Entry<Object, Object> entry : cartData.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("product:")) {
                Long productId = Long.valueOf(key.substring(8)); // Remove "product:" prefix
                CartItemData itemData = (CartItemData) entry.getValue();
                
                CartItemDto item = new CartItemDto(
                    productId,
                    itemData.getProductName(),
                    itemData.getImageUrl(),
                    itemData.getQuantity(),
                    itemData.getPrice(),
                    Instant.parse(itemData.getAddedAt())
                );
                item.calculateTotalPrice();
                
                items.add(item);
                totalAmount = totalAmount.add(item.getTotalPrice());
            }
        }
        
        CartDto cart = new CartDto();
        cart.setCartId(cartKey);
        cart.setUserId(userId);
        cart.setSessionId(sessionId);
        cart.setItems(items);
        cart.setTotalAmount(totalAmount);
        cart.setCreatedAt(Instant.now()); // Would be better to store this in Redis
        cart.setUpdatedAt(Instant.now());
        
        log.debug("Built cart from Redis data - Items: {}, Total: {}", items.size(), totalAmount);
        return cart;
    }
}
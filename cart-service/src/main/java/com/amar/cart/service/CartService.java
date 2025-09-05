package com.amar.cart.service;

import com.amar.dto.*;
import com.amar.cart.client.InventoryServiceClient;
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
import java.util.Optional;
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
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
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
    
    // Add Item to Cart with inventory validation
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
        
        // INVENTORY VALIDATION: Check stock availability
        boolean isAvailable = inventoryServiceClient.checkAvailability(request.getProductId(), newQuantity);
        if (!isAvailable) {
            log.warn("Insufficient stock for product: {} requested: {}", request.getProductId(), newQuantity);
            throw new InsufficientStockException("Insufficient stock available for product: " + product.getName(), 
                request.getProductId(), newQuantity);
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
        
        log.info("Successfully added item to cart with inventory validation - Product: {}, Final Quantity: {}", 
                request.getProductId(), newQuantity);
        
        return getCart(userId, sessionId);
    }
    
    // Update Item Quantity with inventory validation
    public CartDto updateItemQuantity(String userId, String sessionId, UpdateCartItemRequest request) {
        log.info("Updating cart item quantity - User: {}, Product: {}, Quantity: {}", 
                userId, request.getProductId(), request.getQuantity());
        
        String cartKey = getCartKey(userId, sessionId);
        String productKey = "product:" + request.getProductId();
        
        // Check if item exists
        if (!redisTemplate.opsForHash().hasKey(cartKey, productKey)) {
            throw new CartItemNotFoundException("Item not found in cart: " + request.getProductId());
        }
        
        // INVENTORY VALIDATION: Check stock availability for new quantity
        if (request.getQuantity() > 0) {
            boolean isAvailable = inventoryServiceClient.checkAvailability(request.getProductId(), request.getQuantity());
            if (!isAvailable) {
                log.warn("Insufficient stock for product: {} requested: {}", request.getProductId(), request.getQuantity());
                throw new ProductValidationException("Insufficient stock available for the requested quantity");
            }
        }
        
        // Get existing item data
        CartItemData existingItem = (CartItemData) redisTemplate.opsForHash().get(cartKey, productKey);
        existingItem.setQuantity(request.getQuantity());
        
        // Update in Redis
        redisTemplate.opsForHash().put(cartKey, productKey, existingItem);
        
        log.info("Successfully updated item quantity with inventory validation - Product: {}, New Quantity: {}", 
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
    
    // Validate entire cart inventory availability
    public boolean validateCartInventory(String userId, String sessionId) {
        log.debug("Validating cart inventory for user: {}, session: {}", userId, sessionId);
        
        CartDto cart = getCart(userId, sessionId);
        if (cart.getItems().isEmpty()) {
            return true;
        }
        
        // Check availability for each item
        for (CartItemDto item : cart.getItems()) {
            boolean isAvailable = inventoryServiceClient.checkAvailability(item.getProductId(), item.getQuantity());
            if (!isAvailable) {
                log.warn("Cart validation failed - insufficient stock for product: {} quantity: {}", 
                        item.getProductId(), item.getQuantity());
                return false;
            }
        }
        
        log.debug("Cart inventory validation passed for {} items", cart.getItems().size());
        return true;
    }
    
    // Reserve stock for entire cart (for checkout)
    public boolean reserveCartStock(String userId, String sessionId) {
        log.info("Reserving cart stock for user: {}, session: {}", userId, sessionId);
        
        CartDto cart = getCart(userId, sessionId);
        if (cart.getItems().isEmpty()) {
            log.debug("Empty cart - no stock to reserve");
            return true;
        }
        
        // Generate reservation ID from cart
        UUID reservationId = UUID.nameUUIDFromBytes((cart.getCartId() + "_" + Instant.now().toString()).getBytes());
        
        // Convert cart items to reservation items
        List<InventoryServiceClient.StockReservationItem> reservationItems = cart.getItems().stream()
                .map(item -> new InventoryServiceClient.StockReservationItem(item.getProductId(), item.getQuantity()))
                .toList();
        
        boolean reserved = inventoryServiceClient.reserveStock(reservationId, reservationItems, userId);
        
        if (reserved) {
            log.info("Successfully reserved stock for cart - reservation ID: {}", reservationId);
            // Store reservation ID in Redis for cleanup
            storeReservationId(cart.getCartId(), reservationId);
        } else {
            log.error("Failed to reserve stock for cart - user: {}, session: {}", userId, sessionId);
        }
        
        return reserved;
    }
    
    // Release cart stock reservation
    public boolean releaseCartReservation(String userId, String sessionId) {
        log.info("Releasing cart stock reservation for user: {}, session: {}", userId, sessionId);
        
        String cartKey = getCartKey(userId, sessionId);
        UUID reservationId = getReservationId(cartKey);
        
        if (reservationId == null) {
            log.debug("No reservation found for cart: {}", cartKey);
            return true;
        }
        
        boolean released = inventoryServiceClient.releaseReservation(reservationId);
        
        if (released) {
            log.info("Successfully released stock reservation: {}", reservationId);
            removeReservationId(cartKey);
        } else {
            log.error("Failed to release stock reservation: {}", reservationId);
        }
        
        return released;
    }
    
    // Get cart with inventory information for each item
    public CartDto getCartWithInventoryInfo(String userId, String sessionId) {
        CartDto cart = getCart(userId, sessionId);
        
        // Enrich each item with current inventory status
        for (CartItemDto item : cart.getItems()) {
            Optional<InventoryDto> inventory = inventoryServiceClient.getInventoryByProductId(item.getProductId());
            if (inventory.isPresent()) {
                InventoryDto inv = inventory.get();
                // Add inventory metadata to item (could extend CartItemDto for this)
                item.setAvailableStock(inv.getAvailableQuantity());
                item.setInStock(inv.getAvailableQuantity() >= item.getQuantity());
                item.setStockStatus(inv.getStockStatus());
            }
        }
        
        return cart;
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
    
    // Reservation management helpers
    private void storeReservationId(String cartKey, UUID reservationId) {
        String reservationKey = cartKey + ":reservation";
        redisTemplate.opsForValue().set(reservationKey, reservationId.toString(), Duration.ofMinutes(15));
    }
    
    private UUID getReservationId(String cartKey) {
        String reservationKey = cartKey + ":reservation";
        String reservationIdStr = (String) redisTemplate.opsForValue().get(reservationKey);
        return reservationIdStr != null ? UUID.fromString(reservationIdStr) : null;
    }
    
    private void removeReservationId(String cartKey) {
        String reservationKey = cartKey + ":reservation";
        redisTemplate.delete(reservationKey);
    }
}
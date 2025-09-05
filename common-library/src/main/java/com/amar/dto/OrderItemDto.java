package com.amar.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemDto {
    
    private UUID id;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Product name is required")
    private String productName;
    
    @NotBlank(message = "Product SKU is required")
    private String productSku;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
    
    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total price must be greater than 0")
    private BigDecimal totalPrice;
    
    // Product details at time of order
    private String productDescription;
    private String productImageUrl;
    private String productBrand;
    private String productCategory;
    
    // Pricing details
    private BigDecimal listPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    
    // Fulfillment details
    private String fulfillmentStatus;
    private Integer quantityShipped;
    private Integer quantityDelivered;
    private Integer quantityCancelled;
    private Integer quantityReturned;
    
    // Constructors
    public OrderItemDto() {}
    
    public OrderItemDto(UUID orderId, Long productId, String productName, String productSku, 
                       Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductSku() {
        return productSku;
    }
    
    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getProductDescription() {
        return productDescription;
    }
    
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
    
    public String getProductImageUrl() {
        return productImageUrl;
    }
    
    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }
    
    public String getProductBrand() {
        return productBrand;
    }
    
    public void setProductBrand(String productBrand) {
        this.productBrand = productBrand;
    }
    
    public String getProductCategory() {
        return productCategory;
    }
    
    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }
    
    public BigDecimal getListPrice() {
        return listPrice;
    }
    
    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }
    
    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }
    
    public Integer getQuantityShipped() {
        return quantityShipped;
    }
    
    public void setQuantityShipped(Integer quantityShipped) {
        this.quantityShipped = quantityShipped;
    }
    
    public Integer getQuantityDelivered() {
        return quantityDelivered;
    }
    
    public void setQuantityDelivered(Integer quantityDelivered) {
        this.quantityDelivered = quantityDelivered;
    }
    
    public Integer getQuantityCancelled() {
        return quantityCancelled;
    }
    
    public void setQuantityCancelled(Integer quantityCancelled) {
        this.quantityCancelled = quantityCancelled;
    }
    
    public Integer getQuantityReturned() {
        return quantityReturned;
    }
    
    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }
    
    // Helper methods
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isFullyShipped() {
        return quantityShipped != null && quantityShipped.equals(quantity);
    }
    
    public boolean isFullyDelivered() {
        return quantityDelivered != null && quantityDelivered.equals(quantity);
    }
    
    public boolean isPartiallyCancelled() {
        return quantityCancelled != null && quantityCancelled > 0 && quantityCancelled < quantity;
    }
    
    public boolean isFullyCancelled() {
        return quantityCancelled != null && quantityCancelled.equals(quantity);
    }
    
    public Integer getQuantityPending() {
        int shipped = quantityShipped != null ? quantityShipped : 0;
        int cancelled = quantityCancelled != null ? quantityCancelled : 0;
        return Math.max(0, quantity - shipped - cancelled);
    }
}
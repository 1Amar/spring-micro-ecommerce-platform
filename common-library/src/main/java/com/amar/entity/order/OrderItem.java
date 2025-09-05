package com.amar.entity.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items", schema = "order_service")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    // Product information (snapshot at time of order)
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;
    
    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;
    
    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;
    
    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;
    
    @Column(name = "product_brand", length = 200)
    private String productBrand;
    
    @Column(name = "product_category", length = 200)
    private String productCategory;
    
    // Quantity and pricing
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "list_price", precision = 19, scale = 2)
    private BigDecimal listPrice;
    
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;
    
    // Fulfillment tracking
    @Column(name = "fulfillment_status")
    @Enumerated(EnumType.STRING)
    private ItemFulfillmentStatus fulfillmentStatus = ItemFulfillmentStatus.PENDING;
    
    @Column(name = "quantity_shipped")
    private Integer quantityShipped = 0;
    
    @Column(name = "quantity_delivered")
    private Integer quantityDelivered = 0;
    
    @Column(name = "quantity_cancelled")
    private Integer quantityCancelled = 0;
    
    @Column(name = "quantity_returned")
    private Integer quantityReturned = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // JPA lifecycle methods
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public OrderItem() {}
    
    public OrderItem(Order order, Long productId, String productName, String productSku, 
                    Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.order = order;
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
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
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
    
    public ItemFulfillmentStatus getFulfillmentStatus() {
        return fulfillmentStatus;
    }
    
    public void setFulfillmentStatus(ItemFulfillmentStatus fulfillmentStatus) {
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
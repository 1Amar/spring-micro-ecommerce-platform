package com.amar.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotEmpty(message = "Cart ID is required")
    private String cartId;
    
    @NotEmpty(message = "Order items are required")
    private List<CreateOrderItemRequest> items;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    
    // Customer contact information
    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;
    
    private String customerPhone;
    
    // Billing address
    @NotBlank(message = "Billing first name is required")
    private String billingFirstName;
    
    @NotBlank(message = "Billing last name is required")
    private String billingLastName;
    
    private String billingCompany;
    
    @NotBlank(message = "Billing street address is required")
    private String billingStreet;
    
    @NotBlank(message = "Billing city is required")
    private String billingCity;
    
    @NotBlank(message = "Billing state is required")
    private String billingState;
    
    @NotBlank(message = "Billing postal code is required")
    private String billingPostalCode;
    
    @NotBlank(message = "Billing country is required")
    private String billingCountry;
    
    // Shipping address
    @NotBlank(message = "Shipping first name is required")
    private String shippingFirstName;
    
    @NotBlank(message = "Shipping last name is required")
    private String shippingLastName;
    
    private String shippingCompany;
    
    @NotBlank(message = "Shipping street address is required")
    private String shippingStreet;
    
    @NotBlank(message = "Shipping city is required")
    private String shippingCity;
    
    @NotBlank(message = "Shipping state is required")
    private String shippingState;
    
    @NotBlank(message = "Shipping postal code is required")
    private String shippingPostalCode;
    
    @NotBlank(message = "Shipping country is required")
    private String shippingCountry;
    
    // Shipping preferences
    private String shippingMethod;
    private boolean sameAsBilling;
    
    // Optional fields
    private String notes;
    private String couponCode;
    private BigDecimal expectedTotal;
    
    // Constructors
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(String userId, String cartId, List<CreateOrderItemRequest> items, String paymentMethod) {
        this.userId = userId;
        this.cartId = cartId;
        this.items = items;
        this.paymentMethod = paymentMethod;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCartId() {
        return cartId;
    }
    
    public void setCartId(String cartId) {
        this.cartId = cartId;
    }
    
    public List<CreateOrderItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getBillingFirstName() {
        return billingFirstName;
    }
    
    public void setBillingFirstName(String billingFirstName) {
        this.billingFirstName = billingFirstName;
    }
    
    public String getBillingLastName() {
        return billingLastName;
    }
    
    public void setBillingLastName(String billingLastName) {
        this.billingLastName = billingLastName;
    }
    
    public String getBillingCompany() {
        return billingCompany;
    }
    
    public void setBillingCompany(String billingCompany) {
        this.billingCompany = billingCompany;
    }
    
    public String getBillingStreet() {
        return billingStreet;
    }
    
    public void setBillingStreet(String billingStreet) {
        this.billingStreet = billingStreet;
    }
    
    public String getBillingCity() {
        return billingCity;
    }
    
    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }
    
    public String getBillingState() {
        return billingState;
    }
    
    public void setBillingState(String billingState) {
        this.billingState = billingState;
    }
    
    public String getBillingPostalCode() {
        return billingPostalCode;
    }
    
    public void setBillingPostalCode(String billingPostalCode) {
        this.billingPostalCode = billingPostalCode;
    }
    
    public String getBillingCountry() {
        return billingCountry;
    }
    
    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }
    
    public String getShippingFirstName() {
        return shippingFirstName;
    }
    
    public void setShippingFirstName(String shippingFirstName) {
        this.shippingFirstName = shippingFirstName;
    }
    
    public String getShippingLastName() {
        return shippingLastName;
    }
    
    public void setShippingLastName(String shippingLastName) {
        this.shippingLastName = shippingLastName;
    }
    
    public String getShippingCompany() {
        return shippingCompany;
    }
    
    public void setShippingCompany(String shippingCompany) {
        this.shippingCompany = shippingCompany;
    }
    
    public String getShippingStreet() {
        return shippingStreet;
    }
    
    public void setShippingStreet(String shippingStreet) {
        this.shippingStreet = shippingStreet;
    }
    
    public String getShippingCity() {
        return shippingCity;
    }
    
    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }
    
    public String getShippingState() {
        return shippingState;
    }
    
    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }
    
    public String getShippingPostalCode() {
        return shippingPostalCode;
    }
    
    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }
    
    public String getShippingCountry() {
        return shippingCountry;
    }
    
    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }
    
    public String getShippingMethod() {
        return shippingMethod;
    }
    
    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }
    
    public boolean isSameAsBilling() {
        return sameAsBilling;
    }
    
    public void setSameAsBilling(boolean sameAsBilling) {
        this.sameAsBilling = sameAsBilling;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getCouponCode() {
        return couponCode;
    }
    
    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
    
    public BigDecimal getExpectedTotal() {
        return expectedTotal;
    }
    
    public void setExpectedTotal(BigDecimal expectedTotal) {
        this.expectedTotal = expectedTotal;
    }
}
-- =====================================================
-- Create Order Service Schema and Tables
-- Migration: 015-create-order-service-schema.sql
-- Description: Create complete order service database schema
-- =====================================================

-- Create order service schema
CREATE SCHEMA IF NOT EXISTS order_service;

-- =====================================================
-- Orders Table
-- =====================================================
CREATE TABLE order_service.orders (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- Financial information
    total_amount DECIMAL(19,2) NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    tax_amount DECIMAL(19,2) DEFAULT 0.00,
    shipping_cost DECIMAL(19,2) DEFAULT 0.00,
    discount_amount DECIMAL(19,2) DEFAULT 0.00,
    
    -- Payment information
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    payment_method VARCHAR(100),
    payment_transaction_id VARCHAR(255),
    
    -- Shipping information
    shipping_method VARCHAR(100),
    tracking_number VARCHAR(255),
    carrier VARCHAR(100),
    
    -- Customer information
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50),
    
    -- Billing address
    billing_first_name VARCHAR(100),
    billing_last_name VARCHAR(100),
    billing_company VARCHAR(255),
    billing_street TEXT,
    billing_city VARCHAR(100),
    billing_state VARCHAR(50),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(50),
    
    -- Shipping address
    shipping_first_name VARCHAR(100),
    shipping_last_name VARCHAR(100),
    shipping_company VARCHAR(255),
    shipping_street TEXT,
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(50),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(50),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    
    -- Notes and metadata
    notes TEXT,
    admin_notes TEXT,
    cancellation_reason TEXT,
    fulfillment_status VARCHAR(50) DEFAULT 'PENDING'
);

-- =====================================================
-- Order Items Table
-- =====================================================
CREATE TABLE order_service.order_items (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES order_service.orders(id) ON DELETE CASCADE,
    
    -- Product information (snapshot at time of order)
    product_id BIGINT NOT NULL,
    product_name VARCHAR(500) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_description TEXT,
    product_image_url TEXT,
    product_brand VARCHAR(200),
    product_category VARCHAR(200),
    
    -- Quantity and pricing
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(19,2) NOT NULL CHECK (total_price >= 0),
    list_price DECIMAL(19,2),
    discount_amount DECIMAL(19,2) DEFAULT 0.00,
    tax_amount DECIMAL(19,2) DEFAULT 0.00,
    
    -- Fulfillment tracking
    fulfillment_status VARCHAR(50) DEFAULT 'PENDING',
    quantity_shipped INTEGER DEFAULT 0 CHECK (quantity_shipped >= 0),
    quantity_delivered INTEGER DEFAULT 0 CHECK (quantity_delivered >= 0),
    quantity_cancelled INTEGER DEFAULT 0 CHECK (quantity_cancelled >= 0),
    quantity_returned INTEGER DEFAULT 0 CHECK (quantity_returned >= 0),
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Order Status History Table (for audit trail)
-- =====================================================
CREATE TABLE order_service.order_status_history (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES order_service.orders(id) ON DELETE CASCADE,
    
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_by VARCHAR(255),
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Create Indexes for Performance
-- =====================================================

-- Orders table indexes
CREATE INDEX idx_orders_user_id ON order_service.orders(user_id);
CREATE INDEX idx_orders_status ON order_service.orders(status);
CREATE INDEX idx_orders_payment_status ON order_service.orders(payment_status);
CREATE INDEX idx_orders_order_number ON order_service.orders(order_number);
CREATE INDEX idx_orders_created_at ON order_service.orders(created_at DESC);
CREATE INDEX idx_orders_customer_email ON order_service.orders(customer_email);
CREATE INDEX idx_orders_tracking_number ON order_service.orders(tracking_number) WHERE tracking_number IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_orders_user_status_created ON order_service.orders(user_id, status, created_at DESC);
CREATE INDEX idx_orders_status_updated ON order_service.orders(status, updated_at DESC);

-- Order items table indexes
CREATE INDEX idx_order_items_order_id ON order_service.order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_service.order_items(product_id);
CREATE INDEX idx_order_items_sku ON order_service.order_items(product_sku);
CREATE INDEX idx_order_items_fulfillment_status ON order_service.order_items(fulfillment_status);

-- Order status history indexes
CREATE INDEX idx_order_status_history_order_id ON order_service.order_status_history(order_id);
CREATE INDEX idx_order_status_history_created_at ON order_service.order_status_history(created_at DESC);

-- =====================================================
-- Add Constraints
-- =====================================================

-- Order status constraints
ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_order_status_values 
CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'));

-- Payment status constraints
ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_payment_status_values 
CHECK (payment_status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'));

-- Fulfillment status constraints
ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_fulfillment_status_values 
CHECK (fulfillment_status IN ('PENDING', 'PROCESSING', 'PARTIALLY_FULFILLED', 'FULFILLED', 'CANCELLED'));

-- Order item fulfillment status constraints
ALTER TABLE order_service.order_items 
ADD CONSTRAINT chk_item_fulfillment_status_values 
CHECK (fulfillment_status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'));

-- Financial constraints
ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_total_amount_positive CHECK (total_amount > 0),
ADD CONSTRAINT chk_subtotal_positive CHECK (subtotal > 0);

-- Quantity constraints for order items
ALTER TABLE order_service.order_items 
ADD CONSTRAINT chk_quantity_fulfillment_logic 
CHECK (
    quantity_shipped + quantity_cancelled + quantity_returned <= quantity
    AND quantity_delivered <= quantity_shipped
);

-- =====================================================
-- Create Functions and Triggers for Updated At
-- =====================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION order_service.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON order_service.orders
    FOR EACH ROW EXECUTE FUNCTION order_service.update_updated_at_column();

CREATE TRIGGER update_order_items_updated_at
    BEFORE UPDATE ON order_service.order_items
    FOR EACH ROW EXECUTE FUNCTION order_service.update_updated_at_column();

-- =====================================================
-- Create Order Number Generation Function
-- =====================================================

-- Function to generate order numbers with format: ORD-YYYYMMDD-XXXXX
CREATE OR REPLACE FUNCTION order_service.generate_order_number()
RETURNS TEXT AS $$
DECLARE
    date_part TEXT;
    sequence_num INTEGER;
    order_number TEXT;
BEGIN
    -- Get today's date in YYYYMMDD format
    date_part := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    -- Get the next sequence number for today
    SELECT COUNT(*) + 1 INTO sequence_num
    FROM order_service.orders 
    WHERE order_number LIKE 'ORD-' || date_part || '-%'
    AND created_at::DATE = CURRENT_DATE;
    
    -- Format the order number
    order_number := 'ORD-' || date_part || '-' || LPAD(sequence_num::TEXT, 5, '0');
    
    RETURN order_number;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Comments for Documentation
-- =====================================================

COMMENT ON SCHEMA order_service IS 'Schema for order management service';
COMMENT ON TABLE order_service.orders IS 'Main orders table storing order header information';
COMMENT ON TABLE order_service.order_items IS 'Order line items with product snapshots at time of order';
COMMENT ON TABLE order_service.order_status_history IS 'Audit trail for order status changes';

COMMENT ON COLUMN order_service.orders.order_number IS 'Human-readable unique order identifier';
COMMENT ON COLUMN order_service.orders.status IS 'Current order status in the fulfillment workflow';
COMMENT ON COLUMN order_service.orders.payment_status IS 'Current payment processing status';
COMMENT ON COLUMN order_service.orders.fulfillment_status IS 'Physical fulfillment status of the order';

COMMENT ON FUNCTION order_service.generate_order_number() IS 'Generates unique order numbers in format ORD-YYYYMMDD-XXXXX';
COMMENT ON FUNCTION order_service.update_updated_at_column() IS 'Trigger function to automatically update updated_at timestamps';
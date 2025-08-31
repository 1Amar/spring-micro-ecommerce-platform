-- =====================================================
-- Inventory Service Schema Creation
-- Version: 1.0
-- Date: August 31, 2025
-- =====================================================

-- Create inventory service schema
CREATE SCHEMA IF NOT EXISTS inventory_service_schema;

-- Set search path for this schema
SET search_path TO inventory_service_schema, public;

-- =====================================================
-- Main inventory tracking table
-- =====================================================
CREATE TABLE inventory_service_schema.inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    available_quantity INTEGER GENERATED ALWAYS AS (quantity - reserved_quantity) STORED,
    reorder_level INTEGER DEFAULT 10,
    max_stock_level INTEGER DEFAULT 1000,
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic locking
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign key reference to product service (soft reference)
    CONSTRAINT fk_inventory_product_id FOREIGN KEY (product_id) 
        REFERENCES public.products(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT chk_quantity_consistency CHECK (reserved_quantity <= quantity),
    CONSTRAINT chk_reorder_level CHECK (reorder_level >= 0),
    CONSTRAINT chk_max_stock_level CHECK (max_stock_level > reorder_level)
);

-- =====================================================
-- Temporary stock reservations during checkout
-- =====================================================
CREATE TABLE inventory_service_schema.inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id BIGINT NOT NULL,
    order_id UUID NOT NULL UNIQUE,
    session_id VARCHAR(255), -- For anonymous users
    user_id VARCHAR(255), -- For authenticated users
    quantity_reserved INTEGER NOT NULL CHECK (quantity_reserved > 0),
    reserved_by VARCHAR(255) NOT NULL, -- User ID or session ID
    reservation_type VARCHAR(50) NOT NULL DEFAULT 'CHECKOUT', -- CHECKOUT, HOLD, ADMIN
    expires_at TIMESTAMPTZ NOT NULL,
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign key references
    CONSTRAINT fk_reservations_product_id FOREIGN KEY (product_id) 
        REFERENCES public.products(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT chk_reservation_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_user_or_session CHECK (
        (user_id IS NOT NULL AND session_id IS NULL) OR 
        (user_id IS NULL AND session_id IS NOT NULL)
    )
);

-- =====================================================
-- Stock movement audit trail
-- =====================================================
CREATE TYPE inventory_service_schema.movement_type AS ENUM (
    'INBOUND',          -- Stock added (purchase, return)
    'OUTBOUND',         -- Stock removed (sale, damage)
    'RESERVED',         -- Stock reserved for order
    'RESERVATION_RELEASED', -- Reserved stock released back
    'ADJUSTMENT',       -- Manual stock adjustment
    'TRANSFER'          -- Stock transfer between locations
);

CREATE TABLE inventory_service_schema.stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id BIGINT NOT NULL,
    movement_type inventory_service_schema.movement_type NOT NULL,
    quantity INTEGER NOT NULL,
    reference_id UUID, -- Order ID, Purchase Order ID, etc.
    reference_type VARCHAR(50), -- 'ORDER', 'PURCHASE_ORDER', 'ADJUSTMENT', 'TRANSFER'
    reason TEXT,
    performed_by VARCHAR(255), -- User who performed the action
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign key references
    CONSTRAINT fk_movements_product_id FOREIGN KEY (product_id) 
        REFERENCES public.products(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT chk_quantity_not_zero CHECK (quantity != 0)
);

-- =====================================================
-- Low stock alerts configuration
-- =====================================================
CREATE TABLE inventory_service_schema.low_stock_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id BIGINT NOT NULL UNIQUE,
    alert_threshold INTEGER NOT NULL DEFAULT 10,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_alert_sent_at TIMESTAMPTZ,
    alert_frequency_hours INTEGER DEFAULT 24, -- How often to send alerts
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign key references
    CONSTRAINT fk_alerts_product_id FOREIGN KEY (product_id) 
        REFERENCES public.products(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT chk_alert_threshold CHECK (alert_threshold >= 0),
    CONSTRAINT chk_alert_frequency CHECK (alert_frequency_hours > 0)
);

-- =====================================================
-- Performance indexes
-- =====================================================

-- Main inventory table indexes
CREATE INDEX idx_inventory_product_id ON inventory_service_schema.inventory(product_id);
CREATE INDEX idx_inventory_available_quantity ON inventory_service_schema.inventory(available_quantity);
CREATE INDEX idx_inventory_reorder_level ON inventory_service_schema.inventory(quantity) 
    WHERE quantity <= reorder_level;
CREATE INDEX idx_inventory_updated_at ON inventory_service_schema.inventory(updated_at);

-- Reservations table indexes
CREATE INDEX idx_reservations_product_id ON inventory_service_schema.inventory_reservations(product_id);
CREATE INDEX idx_reservations_order_id ON inventory_service_schema.inventory_reservations(order_id);
CREATE INDEX idx_reservations_expires_at ON inventory_service_schema.inventory_reservations(expires_at);
CREATE INDEX idx_reservations_user_id ON inventory_service_schema.inventory_reservations(user_id) 
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_reservations_session_id ON inventory_service_schema.inventory_reservations(session_id) 
    WHERE session_id IS NOT NULL;
CREATE INDEX idx_reservations_expired ON inventory_service_schema.inventory_reservations(is_expired, expires_at);

-- Stock movements indexes
CREATE INDEX idx_movements_product_id ON inventory_service_schema.stock_movements(product_id);
CREATE INDEX idx_movements_created_at ON inventory_service_schema.stock_movements(created_at);
CREATE INDEX idx_movements_reference_id ON inventory_service_schema.stock_movements(reference_id) 
    WHERE reference_id IS NOT NULL;
CREATE INDEX idx_movements_type ON inventory_service_schema.stock_movements(movement_type);

-- Low stock alerts indexes
CREATE INDEX idx_alerts_product_id ON inventory_service_schema.low_stock_alerts(product_id);
CREATE INDEX idx_alerts_enabled ON inventory_service_schema.low_stock_alerts(is_enabled) 
    WHERE is_enabled = TRUE;

-- =====================================================
-- Triggers for automatic timestamp updates
-- =====================================================

-- Update timestamp trigger function (if not exists)
CREATE OR REPLACE FUNCTION inventory_service_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to tables with updated_at columns
CREATE TRIGGER trigger_inventory_updated_at
    BEFORE UPDATE ON inventory_service_schema.inventory
    FOR EACH ROW
    EXECUTE FUNCTION inventory_service_schema.update_updated_at_column();

CREATE TRIGGER trigger_reservations_updated_at
    BEFORE UPDATE ON inventory_service_schema.inventory_reservations
    FOR EACH ROW
    EXECUTE FUNCTION inventory_service_schema.update_updated_at_column();

CREATE TRIGGER trigger_alerts_updated_at
    BEFORE UPDATE ON inventory_service_schema.low_stock_alerts
    FOR EACH ROW
    EXECUTE FUNCTION inventory_service_schema.update_updated_at_column();

-- =====================================================
-- Initial data for testing
-- =====================================================

-- Insert inventory records for some existing products
INSERT INTO inventory_service_schema.inventory (product_id, quantity, reorder_level, max_stock_level)
SELECT 
    p.id,
    FLOOR(RANDOM() * 100) + 10 as quantity, -- Random stock between 10-110
    CASE 
        WHEN RANDOM() > 0.7 THEN 20 
        WHEN RANDOM() > 0.4 THEN 15 
        ELSE 10 
    END as reorder_level,
    CASE 
        WHEN RANDOM() > 0.6 THEN 500 
        ELSE 1000 
    END as max_stock_level
FROM public.products p
WHERE p.is_active = TRUE
LIMIT 1000; -- Initialize inventory for first 1000 active products

-- Create low stock alerts for products with low stock
INSERT INTO inventory_service_schema.low_stock_alerts (product_id, alert_threshold)
SELECT 
    i.product_id,
    i.reorder_level
FROM inventory_service_schema.inventory i
WHERE i.quantity <= i.reorder_level
LIMIT 100;

-- Add some sample stock movements for audit trail
INSERT INTO inventory_service_schema.stock_movements (product_id, movement_type, quantity, reference_type, reason, performed_by)
SELECT 
    i.product_id,
    'INBOUND',
    i.quantity,
    'INITIAL_STOCK',
    'Initial inventory setup',
    'SYSTEM'
FROM inventory_service_schema.inventory i
LIMIT 100;

-- =====================================================
-- Views for common queries
-- =====================================================

-- Low stock products view
CREATE OR REPLACE VIEW inventory_service_schema.v_low_stock_products AS
SELECT 
    i.id,
    i.product_id,
    p.name as product_name,
    p.sku,
    i.quantity,
    i.reserved_quantity,
    i.available_quantity,
    i.reorder_level,
    (i.reorder_level - i.available_quantity) as stock_deficit
FROM inventory_service_schema.inventory i
JOIN public.products p ON i.product_id = p.id
WHERE i.available_quantity <= i.reorder_level
ORDER BY (i.reorder_level - i.available_quantity) DESC;

-- Stock reservations summary view
CREATE OR REPLACE VIEW inventory_service_schema.v_reservation_summary AS
SELECT 
    ir.product_id,
    p.name as product_name,
    COUNT(*) as total_reservations,
    SUM(ir.quantity_reserved) as total_reserved_quantity,
    COUNT(CASE WHEN ir.expires_at > NOW() THEN 1 END) as active_reservations,
    COUNT(CASE WHEN ir.expires_at <= NOW() THEN 1 END) as expired_reservations
FROM inventory_service_schema.inventory_reservations ir
JOIN public.products p ON ir.product_id = p.id
GROUP BY ir.product_id, p.name
ORDER BY total_reserved_quantity DESC;

-- Inventory status summary view
CREATE OR REPLACE VIEW inventory_service_schema.v_inventory_status AS
SELECT 
    COUNT(*) as total_products_tracked,
    COUNT(CASE WHEN available_quantity > reorder_level THEN 1 END) as products_in_stock,
    COUNT(CASE WHEN available_quantity <= reorder_level AND available_quantity > 0 THEN 1 END) as products_low_stock,
    COUNT(CASE WHEN available_quantity = 0 THEN 1 END) as products_out_of_stock,
    SUM(quantity) as total_stock_quantity,
    SUM(reserved_quantity) as total_reserved_quantity,
    SUM(available_quantity) as total_available_quantity
FROM inventory_service_schema.inventory;

COMMENT ON SCHEMA inventory_service_schema IS 'Inventory management schema for e-commerce microservices';
COMMENT ON TABLE inventory_service_schema.inventory IS 'Main inventory tracking with stock levels and reservations';
COMMENT ON TABLE inventory_service_schema.inventory_reservations IS 'Temporary stock reservations during checkout process';
COMMENT ON TABLE inventory_service_schema.stock_movements IS 'Audit trail for all inventory movements and changes';
COMMENT ON TABLE inventory_service_schema.low_stock_alerts IS 'Configuration for low stock alert notifications';
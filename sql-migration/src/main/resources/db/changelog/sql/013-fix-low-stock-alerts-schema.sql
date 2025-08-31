-- =====================================================
-- Alter low_stock_alerts table to match LowStockAlert entity
-- =====================================================

-- Drop existing constraints and indexes that will conflict
DROP INDEX IF EXISTS inventory_service_schema.idx_alerts_enabled;
ALTER TABLE inventory_service_schema.low_stock_alerts DROP CONSTRAINT IF EXISTS fk_alerts_product_id;

-- Backup any existing data (if any)
-- Since this is development, we'll recreate the table structure

-- Drop and recreate table with correct structure
DROP TABLE IF EXISTS inventory_service_schema.low_stock_alerts CASCADE;

CREATE TABLE inventory_service_schema.low_stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    current_stock INTEGER,
    threshold INTEGER,
    message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    
    -- Foreign key references
    CONSTRAINT fk_alerts_product_id FOREIGN KEY (product_id) 
        REFERENCES public.products(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT chk_alert_threshold CHECK (threshold >= 0),
    CONSTRAINT chk_alert_status CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'RESOLVED'))
);

-- Create indexes for performance
CREATE INDEX idx_alerts_product_id ON inventory_service_schema.low_stock_alerts(product_id);
CREATE INDEX idx_alerts_status ON inventory_service_schema.low_stock_alerts(status);
CREATE INDEX idx_alerts_created_at ON inventory_service_schema.low_stock_alerts(created_at);
CREATE INDEX idx_alerts_product_status ON inventory_service_schema.low_stock_alerts(product_id, status);
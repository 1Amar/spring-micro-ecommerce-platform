-- =====================================================
-- Fix movement_type column to work with JPA
-- =====================================================

-- Convert movement_type from PostgreSQL ENUM to VARCHAR
-- This allows JPA @Enumerated(EnumType.STRING) to work properly

ALTER TABLE inventory_service_schema.stock_movements 
ALTER COLUMN movement_type TYPE VARCHAR(50);

-- Add a check constraint to maintain data integrity
ALTER TABLE inventory_service_schema.stock_movements
ADD CONSTRAINT chk_movement_type_values 
CHECK (movement_type IN ('INBOUND', 'OUTBOUND', 'RESERVED', 'RESERVATION_RELEASED', 'ADJUSTMENT', 'TRANSFER'));

-- Drop the old ENUM type (it's no longer used)
DROP TYPE IF EXISTS inventory_service_schema.movement_type;
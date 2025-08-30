-- liquibase formatted sql

-- changeset amar:011
-- comment: Create user_addresses table for user address management

CREATE TABLE user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL DEFAULT 'HOME',
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'United States',
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint to ensure only one default address per user
    CONSTRAINT uk_user_default_address UNIQUE (user_id, is_default) DEFERRABLE INITIALLY DEFERRED
);

-- Create indexes for better performance
CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_user_addresses_default ON user_addresses(user_id, is_default) WHERE is_default = true;
CREATE INDEX idx_user_addresses_type ON user_addresses(user_id, type);

-- Add check constraint for address type
ALTER TABLE user_addresses ADD CONSTRAINT chk_address_type 
    CHECK (type IN ('HOME', 'WORK', 'BILLING', 'SHIPPING', 'OTHER'));

-- Add table comment
COMMENT ON TABLE user_addresses IS 'User address information for shipping and billing';
COMMENT ON COLUMN user_addresses.user_id IS 'Foreign key reference to users table';
COMMENT ON COLUMN user_addresses.type IS 'Address type: HOME, WORK, BILLING, SHIPPING, OTHER';
COMMENT ON COLUMN user_addresses.street IS 'Street address line';
COMMENT ON COLUMN user_addresses.city IS 'City name';
COMMENT ON COLUMN user_addresses.state IS 'State or province';
COMMENT ON COLUMN user_addresses.zip_code IS 'ZIP or postal code';
COMMENT ON COLUMN user_addresses.country IS 'Country name';
COMMENT ON COLUMN user_addresses.is_default IS 'Flag indicating if this is the default address';

-- Function to ensure only one default address per user
CREATE OR REPLACE FUNCTION enforce_single_default_address()
RETURNS TRIGGER AS '
BEGIN
    IF NEW.is_default = true THEN
        -- Set all other addresses for this user to non-default
        UPDATE user_addresses 
        SET is_default = false 
        WHERE user_id = NEW.user_id AND id != COALESCE(NEW.id, -1);
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

-- Create trigger to enforce single default address
CREATE TRIGGER trigger_enforce_single_default_address
    BEFORE INSERT OR UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION enforce_single_default_address();

-- rollback DROP TABLE user_addresses CASCADE; DROP FUNCTION IF EXISTS enforce_single_default_address() CASCADE;
--liquibase formatted sql

--changeset amar:004-create-product-attributes-table
--comment: Create product_attributes table for flexible product properties
CREATE TABLE product_attributes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    attribute_type VARCHAR(50) DEFAULT 'TEXT',
    is_required BOOLEAN DEFAULT FALSE,
    is_variant BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT non_empty_name CHECK (length(trim(name)) > 0),
    CONSTRAINT non_empty_value CHECK (length(trim(value)) > 0),
    CONSTRAINT valid_attribute_type CHECK (attribute_type IN ('TEXT', 'NUMBER', 'BOOLEAN', 'DATE', 'COLOR', 'SIZE', 'MATERIAL'))
);

--rollback DROP TABLE product_attributes;
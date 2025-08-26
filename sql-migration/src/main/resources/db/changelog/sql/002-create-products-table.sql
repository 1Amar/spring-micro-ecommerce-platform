--liquibase formatted sql

--changeset amar:002-create-products-table
--comment: Create products table with comprehensive product information
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(100) UNIQUE NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    price DECIMAL(10,2) NOT NULL,
    compare_at_price DECIMAL(10,2),
    cost DECIMAL(10,2),
    stock_quantity INTEGER DEFAULT 0,
    low_stock_threshold INTEGER DEFAULT 5,
    track_inventory BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    weight DECIMAL(8,3),
    dimensions VARCHAR(100),
    brand VARCHAR(100),
    meta_title VARCHAR(255),
    meta_description TEXT,
    tags TEXT[],
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT positive_price CHECK (price >= 0),
    CONSTRAINT positive_compare_price CHECK (compare_at_price IS NULL OR compare_at_price >= 0),
    CONSTRAINT positive_cost CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT non_negative_stock CHECK (stock_quantity >= 0),
    CONSTRAINT positive_threshold CHECK (low_stock_threshold > 0),
    CONSTRAINT positive_weight CHECK (weight IS NULL OR weight > 0),
    CONSTRAINT valid_sku CHECK (sku ~ '^[A-Z0-9-]+$'),
    CONSTRAINT valid_slug CHECK (slug ~ '^[a-z0-9-]+$'),
    CONSTRAINT non_empty_name CHECK (length(trim(name)) > 0),
    CONSTRAINT non_empty_sku CHECK (length(trim(sku)) > 0),
    CONSTRAINT non_empty_slug CHECK (length(trim(slug)) > 0)
);

--rollback DROP TABLE products;
--liquibase formatted sql

--changeset amar:003-create-product-images-table
--comment: Create product_images table for product media
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    file_size INTEGER,
    file_type VARCHAR(50),
    width INTEGER,
    height INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_url CHECK (url ~ '^https?://.*'),
    CONSTRAINT positive_file_size CHECK (file_size IS NULL OR file_size > 0),
    CONSTRAINT positive_dimensions CHECK (
        (width IS NULL AND height IS NULL) OR 
        (width > 0 AND height > 0)
    )
);

--rollback DROP TABLE product_images;
--liquibase formatted sql

--changeset amar:006-create-indexes
--comment: Create performance indexes for all tables

-- Categories indexes
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_active_order ON categories(is_active, display_order);

-- Products indexes
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_active_featured ON products(is_active, is_featured);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_stock ON products(stock_quantity);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);
CREATE INDEX IF NOT EXISTS idx_products_name_text_search ON products USING gin(to_tsvector('english', name));
CREATE INDEX IF NOT EXISTS idx_products_description_text_search ON products USING gin(to_tsvector('english', description));

-- Product Images indexes removed - using single image per product approach

-- Product Attributes indexes
CREATE INDEX IF NOT EXISTS idx_product_attributes_product_id ON product_attributes(product_id);
CREATE INDEX IF NOT EXISTS idx_product_attributes_name ON product_attributes(name);
CREATE INDEX IF NOT EXISTS idx_product_attributes_variant ON product_attributes(product_id, is_variant);
CREATE INDEX IF NOT EXISTS idx_product_attributes_required ON product_attributes(product_id, is_required);

--rollback DROP INDEX IF EXISTS idx_categories_parent_id;
--rollback DROP INDEX IF EXISTS idx_categories_slug;
--rollback DROP INDEX IF EXISTS idx_categories_active_order;
--rollback DROP INDEX IF EXISTS idx_products_category_id;
--rollback DROP INDEX IF EXISTS idx_products_sku;
--rollback DROP INDEX IF EXISTS idx_products_slug;
--rollback DROP INDEX IF EXISTS idx_products_active_featured;
--rollback DROP INDEX IF EXISTS idx_products_price;
--rollback DROP INDEX IF EXISTS idx_products_stock;
--rollback DROP INDEX IF EXISTS idx_products_brand;
--rollback DROP INDEX IF EXISTS idx_products_created_at;
--rollback DROP INDEX IF EXISTS idx_products_name_text_search;
--rollback DROP INDEX IF EXISTS idx_products_description_text_search;
--rollback DROP INDEX IF EXISTS idx_product_attributes_product_id;
--rollback DROP INDEX IF EXISTS idx_product_attributes_name;
--rollback DROP INDEX IF EXISTS idx_product_attributes_variant;
--rollback DROP INDEX IF EXISTS idx_product_attributes_required;
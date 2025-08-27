-- Add product image fields and rating fields to products table
-- Author: Amar
-- Date: 2025-08-27

-- Add image fields directly to products table (single image per product approach)
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_alt_text VARCHAR(200);

-- Add rating and sales fields for product catalog
ALTER TABLE products ADD COLUMN IF NOT EXISTS stars NUMERIC(3,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_best_seller BOOLEAN DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS bought_in_last_month INTEGER DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_url TEXT;

-- Add performance indexes
CREATE INDEX IF NOT EXISTS idx_products_image_url ON products(image_url) WHERE image_url IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_stars ON products(stars) WHERE stars IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_best_seller ON products(is_best_seller) WHERE is_best_seller = TRUE;
CREATE INDEX IF NOT EXISTS idx_products_active_category ON products(is_active, category_id) WHERE is_active = TRUE;

-- Add comments for documentation
COMMENT ON COLUMN products.image_url IS 'Product image URL - single image per product for simplicity';
COMMENT ON COLUMN products.image_alt_text IS 'Alternative text for product image accessibility';
COMMENT ON COLUMN products.product_url IS 'External product page URL';
COMMENT ON COLUMN products.stars IS 'Average rating (0.0 to 5.0)';
COMMENT ON COLUMN products.review_count IS 'Number of customer reviews';
COMMENT ON COLUMN products.is_best_seller IS 'Best seller flag';
COMMENT ON COLUMN products.bought_in_last_month IS 'Number of purchases in last month';
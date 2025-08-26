--liquibase formatted sql

--changeset amar:005-add-foreign-key-constraints
--comment: Add foreign key constraints for referential integrity

-- Add foreign key constraint for products.category_id
ALTER TABLE products 
ADD CONSTRAINT fk_products_category 
FOREIGN KEY (category_id) REFERENCES categories(id) 
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Add foreign key constraint for product_images.product_id
ALTER TABLE product_images 
ADD CONSTRAINT fk_product_images_product 
FOREIGN KEY (product_id) REFERENCES products(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Add foreign key constraint for product_attributes.product_id
ALTER TABLE product_attributes 
ADD CONSTRAINT fk_product_attributes_product 
FOREIGN KEY (product_id) REFERENCES products(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Add foreign key constraint for categories.parent_id (already exists in table definition, but adding for consistency)
-- This is already handled in the categories table creation, so we'll skip it to avoid conflicts

--rollback ALTER TABLE product_attributes DROP CONSTRAINT IF EXISTS fk_product_attributes_product;
--rollback ALTER TABLE product_images DROP CONSTRAINT IF EXISTS fk_product_images_product;
--rollback ALTER TABLE products DROP CONSTRAINT IF EXISTS fk_products_category;
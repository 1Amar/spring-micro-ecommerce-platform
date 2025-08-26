--liquibase formatted sql

--changeset amar:007-insert-sample-data
--comment: Insert sample categories and products for testing

-- Insert sample categories
INSERT INTO categories (name, description, slug, parent_id, is_active, display_order) VALUES 
('Electronics', 'Electronic devices and accessories', 'electronics', NULL, TRUE, 1),
('Clothing', 'Fashion and apparel', 'clothing', NULL, TRUE, 2),
('Books', 'Books and publications', 'books', NULL, TRUE, 3),
('Home & Garden', 'Home improvement and garden supplies', 'home-garden', NULL, TRUE, 4);

-- Insert subcategories
INSERT INTO categories (name, description, slug, parent_id, is_active, display_order) VALUES 
('Smartphones', 'Mobile phones and accessories', 'smartphones', 1, TRUE, 1),
('Laptops', 'Portable computers', 'laptops', 1, TRUE, 2),
('Mens Clothing', 'Clothing for men', 'mens-clothing', 2, TRUE, 1),
('Womens Clothing', 'Clothing for women', 'womens-clothing', 2, TRUE, 2);

-- Insert sample products
INSERT INTO products (name, description, sku, slug, category_id, price, compare_at_price, stock_quantity, is_active, is_featured, brand) VALUES 
('iPhone 15 Pro', 'Latest iPhone with advanced features', 'IPH15PRO-128', 'iphone-15-pro', 5, 999.99, 1099.99, 50, TRUE, TRUE, 'Apple'),
('Samsung Galaxy S24', 'Premium Android smartphone', 'SGS24-256', 'samsung-galaxy-s24', 5, 899.99, 999.99, 30, TRUE, TRUE, 'Samsung'),
('MacBook Air M3', 'Lightweight laptop with M3 chip', 'MBA-M3-13', 'macbook-air-m3', 6, 1199.99, 1299.99, 25, TRUE, TRUE, 'Apple'),
('Dell XPS 13', 'Premium ultrabook', 'DELLXPS13-I7', 'dell-xps-13', 6, 1099.99, 1199.99, 15, TRUE, FALSE, 'Dell'),
('Classic T-Shirt', 'Comfortable cotton t-shirt', 'TSHIRT-001-M', 'classic-t-shirt', 7, 19.99, 24.99, 100, TRUE, FALSE, 'Generic'),
('Elegant Dress', 'Beautiful evening dress', 'DRESS-001-M', 'elegant-dress', 8, 89.99, 109.99, 20, TRUE, TRUE, 'Fashion Co');

-- Insert sample product images
INSERT INTO product_images (product_id, url, alt_text, is_primary, sort_order) VALUES 
(1, 'https://example.com/images/iphone15pro-front.jpg', 'iPhone 15 Pro front view', TRUE, 1),
(1, 'https://example.com/images/iphone15pro-back.jpg', 'iPhone 15 Pro back view', FALSE, 2),
(2, 'https://example.com/images/galaxy-s24-front.jpg', 'Samsung Galaxy S24 front view', TRUE, 1),
(3, 'https://example.com/images/macbook-air-m3.jpg', 'MacBook Air M3', TRUE, 1),
(4, 'https://example.com/images/dell-xps-13.jpg', 'Dell XPS 13', TRUE, 1),
(5, 'https://example.com/images/classic-tshirt.jpg', 'Classic T-Shirt', TRUE, 1),
(6, 'https://example.com/images/elegant-dress.jpg', 'Elegant Dress', TRUE, 1);

-- Insert sample product attributes
INSERT INTO product_attributes (product_id, name, value, attribute_type, is_variant) VALUES 
(1, 'Storage', '128GB', 'TEXT', TRUE),
(1, 'Color', 'Space Gray', 'COLOR', TRUE),
(1, 'Screen Size', '6.1 inches', 'TEXT', FALSE),
(2, 'Storage', '256GB', 'TEXT', TRUE),
(2, 'Color', 'Phantom Black', 'COLOR', TRUE),
(3, 'Memory', '16GB', 'TEXT', FALSE),
(3, 'Storage', '512GB SSD', 'TEXT', TRUE),
(5, 'Size', 'M', 'SIZE', TRUE),
(5, 'Material', '100% Cotton', 'MATERIAL', FALSE),
(6, 'Size', 'M', 'SIZE', TRUE),
(6, 'Color', 'Navy Blue', 'COLOR', TRUE);

--rollback DELETE FROM product_attributes WHERE product_id IN (1,2,3,5,6);
--rollback DELETE FROM product_images WHERE product_id IN (1,2,3,4,5,6);
--rollback DELETE FROM products WHERE id IN (1,2,3,4,5,6);
--rollback DELETE FROM categories WHERE id IN (1,2,3,4,5,6,7,8);
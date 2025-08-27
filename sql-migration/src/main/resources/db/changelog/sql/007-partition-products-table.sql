-- Partition products table by category for performance with 1.4M+ records
-- Author: Amar  
-- Date: 2025-08-27

-- Create partitioned products table
-- Note: This is for future optimization. For initial implementation, we'll start with regular table and add partitioning later.

-- Analyze current category distribution first
-- This script prepares for future partitioning when dataset grows

-- Create function to get category stats
CREATE OR REPLACE FUNCTION get_category_product_stats()
RETURNS TABLE (
    category_id BIGINT,
    product_count BIGINT,
    category_name TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.category_id,
        COUNT(*) as product_count,
        c.name as category_name
    FROM products p
    LEFT JOIN categories c ON p.category_id = c.id
    GROUP BY p.category_id, c.name
    ORDER BY product_count DESC;
END;
$$ LANGUAGE plpgsql;

-- Create view for performance monitoring
CREATE OR REPLACE VIEW v_product_performance_stats AS
SELECT 
    COUNT(*) as total_products,
    COUNT(DISTINCT category_id) as total_categories,
    AVG(stars) as avg_rating,
    COUNT(*) FILTER (WHERE is_best_seller = TRUE) as bestseller_count,
    MIN(created_at) as oldest_product,
    MAX(created_at) as newest_product
FROM products;

-- Future partitioning strategy (commented out - implement when needed):
/*
-- Step 1: Create partitioned table
CREATE TABLE products_partitioned (
    LIKE products INCLUDING ALL
) PARTITION BY LIST (category_id);

-- Step 2: Create partitions for top categories
CREATE TABLE products_part_electronics PARTITION OF products_partitioned 
    FOR VALUES IN (75, 76, 77, 78, 79, 80, 81, 82, 83);

CREATE TABLE products_part_clothing PARTITION OF products_partitioned 
    FOR VALUES IN (84, 91, 110, 116, 225);

CREATE TABLE products_part_home PARTITION OF products_partitioned 
    FOR VALUES IN (152, 153, 154, 155, 156, 157, 158, 159);

-- Step 3: Create default partition
CREATE TABLE products_part_default PARTITION OF products_partitioned DEFAULT;

-- Step 4: Migrate data (requires downtime)
-- INSERT INTO products_partitioned SELECT * FROM products;
-- DROP TABLE products;
-- ALTER TABLE products_partitioned RENAME TO products;
*/

COMMENT ON FUNCTION get_category_product_stats() IS 'Analyzes product distribution across categories for partitioning decisions';
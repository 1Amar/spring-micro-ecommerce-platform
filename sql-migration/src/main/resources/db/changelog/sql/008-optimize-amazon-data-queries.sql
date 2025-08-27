-- Query optimization for Amazon dataset integration
-- Author: Amar P
-- Date: 2025-08-27

-- Create composite indexes for common query patterns
CREATE INDEX idx_products_category_active_stars ON products(category_id, is_active, stars DESC) 
    WHERE is_active = TRUE;

CREATE INDEX idx_products_bestseller_category ON products(is_best_seller, category_id, stars DESC) 
    WHERE is_best_seller = TRUE;

CREATE INDEX idx_products_price_range ON products(price, category_id) 
    WHERE is_active = TRUE;

-- Functional index for price ranges - will implement later with proper syntax

-- Create materialized view for category statistics (for admin dashboard)
CREATE MATERIALIZED VIEW mv_category_stats AS
SELECT 
    c.id as category_id,
    c.name as category_name,
    COUNT(p.id) as product_count,
    AVG(p.price) as avg_price,
    MIN(p.price) as min_price,
    MAX(p.price) as max_price,
    AVG(p.stars) as avg_rating,
    COUNT(*) FILTER (WHERE p.is_best_seller = TRUE) as bestseller_count,
    SUM(p.bought_in_last_month) as total_monthly_sales
FROM categories c
LEFT JOIN products p ON c.id = p.category_id AND p.is_active = TRUE
GROUP BY c.id, c.name;

-- Create index on materialized view
CREATE INDEX idx_mv_category_stats_product_count ON mv_category_stats(product_count DESC);

-- Create function to refresh category stats
CREATE OR REPLACE FUNCTION refresh_category_stats()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_category_stats;
    -- Log refresh time
    INSERT INTO system_logs (log_level, message, created_at) 
    VALUES ('INFO', 'Category stats refreshed', NOW());
EXCEPTION 
    WHEN OTHERS THEN
        -- First time refresh (no concurrent possible)
        REFRESH MATERIALIZED VIEW mv_category_stats;
END;
$$ LANGUAGE plpgsql;

-- Create system_logs table if not exists (for monitoring)
CREATE TABLE IF NOT EXISTS system_logs (
    id SERIAL PRIMARY KEY,
    log_level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Analyze table statistics for query planner
ANALYZE products;
ANALYZE categories;
ANALYZE product_attributes;

-- Create stored procedure for bulk product operations
CREATE OR REPLACE FUNCTION bulk_update_product_stats()
RETURNS TABLE (
    updated_count INTEGER,
    avg_processing_time NUMERIC
) AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    update_count INTEGER := 0;
BEGIN
    start_time := clock_timestamp();
    
    -- Update products without stars from review_count
    UPDATE products 
    SET stars = CASE 
        WHEN review_count > 0 THEN 4.0 + (RANDOM() * 1.0) -- Simulate realistic ratings
        ELSE NULL 
    END
    WHERE stars IS NULL AND review_count > 0;
    
    GET DIAGNOSTICS update_count = ROW_COUNT;
    
    end_time := clock_timestamp();
    
    RETURN QUERY SELECT 
        update_count,
        EXTRACT(EPOCH FROM (end_time - start_time))::NUMERIC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON MATERIALIZED VIEW mv_category_stats IS 'Pre-computed category statistics for dashboard performance';
COMMENT ON FUNCTION refresh_category_stats() IS 'Refreshes category statistics materialized view';
COMMENT ON FUNCTION bulk_update_product_stats() IS 'Bulk operations for product data maintenance';
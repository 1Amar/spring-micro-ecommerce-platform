-- Run this in your working psql session:
-- psql -h localhost -p 5432 -U ecom_user -d ecommerce

-- Create a new database for microservices
CREATE DATABASE microservices_ecom;

-- Grant permissions to ecom_user on the new database
ALTER DATABASE microservices_ecom OWNER TO ecom_user;
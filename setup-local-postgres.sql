-- Setup script for local PostgreSQL
-- Run this in pgAdmin or psql as a superuser

-- Create database for microservices
CREATE DATABASE microservices_ecom;

-- Create users for different services
CREATE USER keycloak_user WITH PASSWORD 'keycloak_pass';
CREATE USER product_user WITH PASSWORD 'product_pass';
CREATE USER app_user WITH PASSWORD 'app_password';

-- Grant permissions on microservices_ecom database
GRANT ALL PRIVILEGES ON DATABASE microservices_ecom TO keycloak_user;
GRANT ALL PRIVILEGES ON DATABASE microservices_ecom TO product_user;
GRANT ALL PRIVILEGES ON DATABASE microservices_ecom TO app_user;

-- Connect to the new database to create schemas
\c microservices_ecom;

-- Create schemas for different services
CREATE SCHEMA keycloak;
CREATE SCHEMA product_service;
CREATE SCHEMA inventory_service;
CREATE SCHEMA order_service;
CREATE SCHEMA payment_service;
CREATE SCHEMA notification_service;

-- Grant schema permissions
GRANT ALL ON SCHEMA keycloak TO keycloak_user;
GRANT ALL ON SCHEMA product_service TO product_user;
GRANT ALL ON SCHEMA product_service TO app_user;
GRANT ALL ON SCHEMA inventory_service TO app_user;
GRANT ALL ON SCHEMA order_service TO app_user;
GRANT ALL ON SCHEMA payment_service TO app_user;
GRANT ALL ON SCHEMA notification_service TO app_user;

-- Grant usage and create permissions
GRANT USAGE, CREATE ON SCHEMA keycloak TO keycloak_user;
GRANT USAGE, CREATE ON SCHEMA product_service TO product_user;
GRANT USAGE, CREATE ON SCHEMA product_service TO app_user;
GRANT USAGE, CREATE ON SCHEMA inventory_service TO app_user;
GRANT USAGE, CREATE ON SCHEMA order_service TO app_user;
GRANT USAGE, CREATE ON SCHEMA payment_service TO app_user;
GRANT USAGE, CREATE ON SCHEMA notification_service TO app_user;

-- Show created databases and schemas
\l
\dn
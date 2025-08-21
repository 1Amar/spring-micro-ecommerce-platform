-- Script to verify database setup
-- Run this with: docker exec -i postgres psql -U devuser -d ecommerce_dev < check-databases.sql

-- List all databases
\l

-- List all users
\du

-- Connect to keycloak database and check
\c keycloak
\dt

-- Check keycloak user permissions
SELECT datname, datallowconn, datacl FROM pg_database WHERE datname = 'keycloak';
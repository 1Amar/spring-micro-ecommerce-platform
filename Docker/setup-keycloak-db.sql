-- Setup Keycloak database in Windows PostgreSQL
-- Run this in your existing Windows PostgreSQL instance

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Create Keycloak user with password
CREATE USER keycloak_user WITH ENCRYPTED PASSWORD 'keycloak_pass';

-- Grant all privileges on keycloak database to keycloak_user
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_user;

-- Connect to keycloak database and grant schema privileges
\c keycloak;
GRANT ALL ON SCHEMA public TO keycloak_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO keycloak_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO keycloak_user;

-- Verify setup
\l
\du

-- Test connection (optional)
-- You can test: psql -U keycloak_user -d keycloak -h localhost -p 5432
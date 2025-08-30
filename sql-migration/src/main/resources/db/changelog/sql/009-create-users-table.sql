-- liquibase formatted sql

-- changeset amar:009
-- comment: Create users table with Keycloak integration

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(320) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;

-- Add table comment
COMMENT ON TABLE users IS 'Core user table integrated with Keycloak authentication';
COMMENT ON COLUMN users.keycloak_id IS 'Unique identifier from Keycloak authentication server';
COMMENT ON COLUMN users.email IS 'User email address, unique across the system';
COMMENT ON COLUMN users.username IS 'Username for display purposes, unique across the system';
COMMENT ON COLUMN users.is_active IS 'Flag to indicate if user account is active';

-- rollback DROP TABLE users CASCADE;
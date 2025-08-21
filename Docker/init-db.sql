-- Create databases for the microservices platform
CREATE DATABASE keycloak;
CREATE DATABASE ecommerce_app;
CREATE DATABASE inventory_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;

-- Create users
CREATE USER keycloak_user WITH ENCRYPTED PASSWORD 'keycloak_pass';
CREATE USER app_user WITH ENCRYPTED PASSWORD 'app_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_user;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_app TO app_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO app_user;
GRANT ALL PRIVILEGES ON DATABASE product_db TO app_user;
GRANT ALL PRIVILEGES ON DATABASE order_db TO app_user;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO app_user;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO app_user;

-- Switch to keycloak database and grant schema privileges
\c keycloak;
GRANT ALL ON SCHEMA public TO keycloak_user;
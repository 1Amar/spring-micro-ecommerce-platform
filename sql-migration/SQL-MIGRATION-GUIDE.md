# SQL Migration Guide

## Overview
This module contains Liquibase database migrations for the Spring Boot microservices e-commerce platform. It manages database schema changes and sample data insertion across all microservices.

## Project Structure
```
sql-migration/
├── pom.xml                                    # Maven configuration with Liquibase plugin
├── src/main/resources/
│   ├── liquibase.properties                  # Database connection configuration
│   └── db/changelog/
│       ├── db.changelog-master.yaml          # Main changelog file
│       └── sql/
│           ├── 001-create-categories-table.sql
│           ├── 002-create-products-table.sql
│           ├── 003-create-product-images-table.sql
│           ├── 004-create-product-attributes-table.sql
│           ├── 005-add-foreign-key-constraints.sql
│           ├── 006-create-indexes.sql
│           └── 007-insert-sample-data.sql
```

## Database Schema

### Tables Created
1. **categories** - Product categories with hierarchical structure
2. **products** - Main product catalog table
3. **product_images** - Product image metadata (URLs, alt text, dimensions)
4. **product_attributes** - Dynamic product attributes (size, color, etc.)

### Key Features
- **Foreign Key Constraints** - Proper referential integrity
- **Performance Indexes** - Optimized for common queries
- **Sample Data** - Ready-to-use test data for development
- **Validation Constraints** - Data integrity at database level

## Usage Instructions

### Prerequisites
- PostgreSQL database running on localhost:5432
- Database `microservices_ecom` created with user `ecom_user`

### Running Migrations

#### 1. Update Database Configuration
Edit `src/main/resources/liquibase.properties` if needed:
```properties
url=jdbc:postgresql://localhost:5432/microservices_ecom
username=ecom_user
password=ecom_pass
```

#### 2. Run All Migrations
```bash
cd sql-migration
mvn clean liquibase:update
```

#### 3. Verify Migration Status
```bash
mvn liquibase:status
```

#### 4. Generate Migration Report
```bash
mvn liquibase:updateSQL > migration-preview.sql
```

### Development Workflow

#### Adding New Migrations
1. Create new SQL file in `src/main/resources/db/changelog/sql/`
2. Follow naming convention: `XXX-descriptive-name.sql`
3. Add changeset to `db.changelog-master.yaml`

Example:
```sql
--liquibase formatted sql

--changeset yourname:008-add-user-preferences
--comment: Add user preferences table
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE user_preferences;
```

#### Rolling Back Changes
```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific date
mvn liquibase:rollback -Dliquibase.rollbackDate=2025-08-26
```

## Sample Data Overview

The migration includes comprehensive sample data:

### Categories (15 items)
- Electronics (Smartphones, Laptops, Tablets)
- Fashion (Men's, Women's, Accessories)
- Home & Garden (Furniture, Appliances, Decor)
- Books (Fiction, Non-fiction, Technical)
- Sports & Outdoors (Fitness, Camping, Sports Equipment)

### Products (20 items)
- Complete product information with pricing
- Stock quantities and inventory tracking
- SEO-friendly slugs and metadata
- Realistic product descriptions

### Product Images (40 items)
- Multiple images per product
- Primary image designation
- Alt text for accessibility
- Proper dimensions metadata

### Product Attributes (60 items)
- Dynamic attributes like Size, Color, Material
- Filterable and searchable attributes
- Proper data types and validation

## Troubleshooting

### Common Issues

#### 1. Connection Refused
```
Error: Connection refused to localhost:5432
```
**Solution:** Ensure PostgreSQL is running and accessible

#### 2. Database Does Not Exist
```
Error: database "microservices_ecom" does not exist
```
**Solution:** Create database first:
```sql
CREATE DATABASE microservices_ecom;
CREATE USER ecom_user WITH PASSWORD 'ecom_pass';
GRANT ALL PRIVILEGES ON DATABASE microservices_ecom TO ecom_user;
```

#### 3. Permission Denied
```
Error: permission denied for schema public
```
**Solution:** Grant proper permissions:
```sql
GRANT USAGE ON SCHEMA public TO ecom_user;
GRANT CREATE ON SCHEMA public TO ecom_user;
```

#### 4. Migration Already Applied
```
Error: Changeset already exists
```
**Solution:** Use force update or clear checksums:
```bash
mvn liquibase:clearCheckSums
mvn liquibase:update
```

### Validation Commands

#### Check Database State
```bash
# Connect to database and verify tables
psql -h localhost -U ecom_user -d microservices_ecom -c "\dt"

# Count records in each table
psql -h localhost -U ecom_user -d microservices_ecom -c "
SELECT 
    'categories' as table_name, COUNT(*) as record_count FROM categories
UNION ALL
SELECT 
    'products' as table_name, COUNT(*) as record_count FROM products
UNION ALL
SELECT 
    'product_images' as table_name, COUNT(*) as record_count FROM product_images
UNION ALL
SELECT 
    'product_attributes' as table_name, COUNT(*) as record_count FROM product_attributes;
"
```

#### Verify Foreign Keys
```sql
SELECT 
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.constraint_column_usage AS ccu
    ON tc.constraint_name = ccu.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public';
```

## Integration with Microservices

### ProductService Configuration
The ProductService automatically uses these tables through JPA entities:
- `Product` entity maps to `products` table
- `Category` entity maps to `categories` table
- Proper relationships configured with `@ManyToOne` and `@OneToMany`

### Entity-Database Mapping
- Column names use snake_case (database) vs camelCase (Java entities)
- MapStruct handles DTO conversion automatically
- Spring Data JPA provides repository operations

## Best Practices

### 1. Migration Guidelines
- **Always test migrations** on development database first
- **Use descriptive changeset IDs** with your name/initials
- **Include rollback statements** for all structural changes
- **Keep migrations small** and focused on single purpose

### 2. Data Safety
- **Backup database** before running migrations in production
- **Use transactions** for complex multi-step changes
- **Validate data** after migrations complete

### 3. Performance Considerations
- **Create indexes** for frequently queried columns
- **Use constraints** to ensure data integrity
- **Monitor query performance** after schema changes

## Future Enhancements

### Planned Features
- **Multi-environment support** (dev, staging, prod configurations)
- **Data seeding scripts** for different environments
- **Migration validation tests** with TestContainers
- **Performance monitoring** for migration execution

### Schema Evolution
- **User management tables** for authentication
- **Order processing tables** for e-commerce workflow
- **Inventory tracking** with real-time updates
- **Search optimization** with full-text search indexes

---

For questions or issues with database migrations, check the logs in `target/liquibase/` directory or contact the development team.
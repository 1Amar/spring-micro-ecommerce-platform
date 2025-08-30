# AWS Deployment Strategy

## Overview
Production deployment strategy using AWS managed services with single RDS PostgreSQL database.

## AWS Architecture
```
AWS Production:
├── AWS RDS PostgreSQL (Multi-AZ)
│   ├── microservices_ecom (database) - All e-commerce data
│   └── keycloak (database) - Authentication data
├── ECS/EKS Cluster
│   ├── Keycloak Service
│   ├── User Service
│   ├── Product Service
│   ├── API Gateway Service
│   └── Other Microservices
├── Application Load Balancer (ALB)
├── S3 (Static Assets)
├── CloudFront (CDN)
└── Route 53 (DNS)
```

## Benefits of Single RDS Database
✅ **Cost Effective**: Single RDS instance vs multiple databases
✅ **Simplified Management**: One backup, monitoring, scaling strategy  
✅ **Better Performance**: Local transactions, no cross-database queries
✅ **Easier Migrations**: Single liquibase migration process
✅ **Consistent Connectivity**: All services use same connection parameters

## RDS Database Configuration

### RDS Instance Specifications
```yaml
Database Engine: PostgreSQL 15+
Instance Class: db.t3.medium (for start)
Storage: GP3 SSD, 100GB initial (auto-scaling enabled)
Multi-AZ: Yes (for high availability)
Backup: Automated, 7-day retention
Monitoring: Enhanced monitoring enabled
```

### Database Structure
```sql
-- Single RDS instance with multiple databases:
CREATE DATABASE microservices_ecom;  -- All microservices data
CREATE DATABASE keycloak;            -- Keycloak authentication
CREATE DATABASE keycloak_backup;     -- Emergency backup

-- Users for different services
CREATE USER ecom_app_user WITH PASSWORD 'secure_password';
CREATE USER keycloak_user WITH PASSWORD 'secure_password';
CREATE USER backup_user WITH PASSWORD 'secure_password';

-- Permissions
GRANT ALL ON DATABASE microservices_ecom TO ecom_app_user;
GRANT ALL ON DATABASE keycloak TO keycloak_user;
```

## Service Configuration

### Environment Variables (All Services)
```bash
# Database Connection
DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
DB_PORT=5432
DB_NAME=microservices_ecom
DB_USER=ecom_app_user
DB_PASSWORD=${DB_PASSWORD_SECRET}

# Keycloak specific
KC_DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
KC_DB_NAME=keycloak
KC_DB_USER=keycloak_user
KC_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD_SECRET}
```

### Security Best Practices
- ✅ Database passwords stored in AWS Secrets Manager
- ✅ RDS security groups restrict access to ECS/EKS cluster only
- ✅ SSL/TLS encryption for all database connections
- ✅ VPC private subnets for database access
- ✅ IAM roles for service authentication

## Migration Strategy

### From Local to AWS
1. **Database Export**: Export local PostgreSQL data
2. **RDS Setup**: Create RDS instance with required databases
3. **Data Migration**: Import data to RDS
4. **Service Deployment**: Deploy services with RDS connection
5. **DNS Cutover**: Update DNS to point to AWS ALB

### Database Migration Commands
```bash
# Export local data
pg_dump -U ecom_user -d microservices_ecom > microservices_ecom_backup.sql
pg_dump -U keycloak_user -d keycloak > keycloak_backup.sql

# Import to RDS
psql -h your-rds-endpoint -U ecom_app_user -d microservices_ecom < microservices_ecom_backup.sql
psql -h your-rds-endpoint -U keycloak_user -d keycloak < keycloak_backup.sql
```

## Container Deployment

### Docker Images
```dockerfile
# Each service gets its own Docker image
user-service:latest
product-service:latest  
keycloak:22.0.1
api-gateway:latest
angular-frontend:latest
```

### ECS Task Definitions
```json
{
  "family": "user-service",
  "taskRoleArn": "arn:aws:iam::account:role/ecom-task-role",
  "environment": [
    {"name": "DB_HOST", "value": "${RDS_ENDPOINT}"},
    {"name": "DB_NAME", "value": "microservices_ecom"}
  ],
  "secrets": [
    {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"}
  ]
}
```

## Monitoring & Observability

### AWS Native Services
- **CloudWatch Logs**: Application and database logs
- **CloudWatch Metrics**: Custom metrics and alerts  
- **X-Ray**: Distributed tracing
- **RDS Performance Insights**: Database performance monitoring

### Custom Dashboards
- Application performance metrics
- Database connection pool metrics
- User authentication success/failure rates
- API response times and error rates

## Cost Optimization

### Database Costs
- Single RDS instance vs multiple: ~60% cost reduction
- Use Reserved Instances for predictable workloads
- Enable automated storage scaling
- Implement connection pooling

### Compute Costs  
- Use Spot Instances for non-critical services
- Auto Scaling for variable load
- Right-size instances based on actual usage

## Disaster Recovery

### Backup Strategy
- Automated RDS backups (7-day retention)
- Manual snapshots before major deployments
- Cross-region backup replication for critical data

### Recovery Procedures
1. **RDS Failure**: Automatic failover to Multi-AZ standby
2. **Data Corruption**: Point-in-time recovery from automated backups
3. **Region Failure**: Manual recovery from cross-region backups

## Security Considerations

### Network Security
- Private VPC with isolated database subnets
- Security groups allowing only necessary traffic
- NAT Gateway for outbound internet access

### Data Security
- Encryption at rest (RDS encrypted storage)
- Encryption in transit (SSL/TLS connections)
- Regular security patches and updates

### Access Control
- IAM roles for service-to-service authentication
- Least privilege access principles
- Regular access reviews and rotation

## Performance Optimization

### Database Performance
- Connection pooling (HikariCP) with optimal settings
- Database indexing strategy
- Query optimization and monitoring
- Read replicas for read-heavy workloads

### Application Performance
- Caching with ElastiCache Redis
- CDN for static assets
- Load balancer health checks
- Auto-scaling policies

## Deployment Checklist

### Pre-Deployment
- [ ] RDS instance created and configured
- [ ] Security groups configured
- [ ] Secrets Manager setup
- [ ] Container images built and pushed
- [ ] Environment variables configured

### Deployment
- [ ] Database schema migrated
- [ ] Services deployed to ECS/EKS
- [ ] Load balancer configured
- [ ] DNS records updated
- [ ] SSL certificates configured

### Post-Deployment
- [ ] Health checks passing
- [ ] Monitoring dashboards working
- [ ] Backup strategy verified
- [ ] Performance benchmarks met
- [ ] Security scan completed

## Rollback Strategy
1. **Database Issues**: Point-in-time recovery
2. **Application Issues**: Previous container image deployment
3. **DNS Issues**: Route 53 record rollback
4. **Complete Failure**: Restore from cross-region backup
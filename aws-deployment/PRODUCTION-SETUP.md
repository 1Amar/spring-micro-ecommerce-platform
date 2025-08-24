# 🌐 Production Deployment with Domain & HTTPS

## 🏗️ Architecture Overview

```
Internet → Route 53 → Elastic IPs → Nginx (HTTPS) → Services
```

### Server Layout
- **Infrastructure Server**: Nginx, Keycloak, PostgreSQL, Monitoring
- **Services Server**: All microservices + Angular frontend

## 🔧 Step-by-Step Production Setup

### Phase 1: Static IP Configuration
```bash
# Allocate Elastic IPs for both servers
aws ec2 allocate-address --domain vpc --tag-specifications 'ResourceType=elastic-ip,Tags=[{Key=Name,Value=ecommerce-infrastructure}]'
aws ec2 allocate-address --domain vpc --tag-specifications 'ResourceType=elastic-ip,Tags=[{Key=Name,Value=ecommerce-services}]'

# Associate with instances
aws ec2 associate-address --instance-id i-0923c080b3c95127d --allocation-id eipalloc-xxxxx
aws ec2 associate-address --instance-id i-07dcf8b2b66592293 --allocation-id eipalloc-xxxxx
```

### Phase 2: Domain Configuration
What's your GoDaddy domain name? I'll create the Route 53 configuration.

**DNS Records Needed:**
```
api.yourdomain.com      → Infrastructure Server IP
auth.yourdomain.com     → Infrastructure Server IP (Keycloak)
app.yourdomain.com      → Services Server IP (Frontend)
monitor.yourdomain.com  → Infrastructure Server IP (Grafana)
```

### Phase 3: Nginx + HTTPS Setup
```bash
# Install Nginx and Certbot
sudo yum install -y nginx
sudo yum install -y certbot python3-certbot-nginx

# Get SSL certificates
sudo certbot --nginx -d api.yourdomain.com -d auth.yourdomain.com -d monitor.yourdomain.com

# Nginx configuration for reverse proxy
# Will be auto-configured based on your domain
```

### Phase 4: Keycloak HTTPS Configuration
```yaml
# Keycloak environment variables
KEYCLOAK_HOSTNAME: auth.yourdomain.com
KEYCLOAK_HOSTNAME_STRICT_HTTPS: true
KC_PROXY: edge
KC_HOSTNAME_STRICT: false
```

## 🌐 Service URLs (After Setup)
| Service | URL | Server |
|---------|-----|--------|
| API Gateway | https://api.yourdomain.com | Infrastructure |
| Keycloak | https://auth.yourdomain.com | Infrastructure |
| Frontend | https://app.yourdomain.com | Services |
| Grafana | https://monitor.yourdomain.com | Infrastructure |
| Prometheus | https://monitor.yourdomain.com/prometheus | Infrastructure |
| Jaeger | https://monitor.yourdomain.com/jaeger | Infrastructure |

## 💰 Additional Costs
- 2x Elastic IPs: $7.30/month ($3.65 each)
- Route 53 Hosted Zone: $0.50/month
- **Total Additional**: ~$8/month

## 🔒 Security Benefits
- ✅ HTTPS everywhere (SSL certificates)
- ✅ Static IPs (no more dynamic IP issues)  
- ✅ Professional domain names
- ✅ Nginx security headers
- ✅ Rate limiting and DDoS protection
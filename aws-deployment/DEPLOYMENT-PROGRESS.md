# 🚀 AWS Deployment Progress Guide

## 📊 Current Status: 70% Complete

### ✅ Phase 1: Infrastructure Setup (COMPLETED)
- [x] AWS credentials configured (Mumbai region: ap-south-1)
- [x] Terraform initialized and validated
- [x] Key pair authentication working
- [x] Network infrastructure deployed (VPC, Security Groups, etc.)

### ✅ Phase 2: Core Servers (COMPLETED)
- [x] **Gateway Server**: t3.medium (4GB RAM) - `13.232.110.186`
  - Docker installed and running
  - Repository cloned at `/opt/ecommerce`
  - Ready for API Gateway, Keycloak, monitoring services
  
- [x] **Services Server**: c7i-flex.large (8GB RAM) - `43.205.233.120`  
  - Docker installed and running
  - Repository cloned at `/opt/ecommerce`
  - Ready for all microservices

### 🚧 Phase 3: Jenkins CI/CD (IN PROGRESS)
- [x] Terraform configuration prepared for us-east-1
- [x] Key pair created: `terraform-user-us-east.pem`
- [ ] **PENDING**: Deploy Jenkins server in us-east-1 region
  - Issue: AWS Free Tier allows max 2 EC2 instances per region
  - Solution: Deploy Jenkins in separate region (us-east-1)

### 🚧 Phase 4: Application Deployment (PENDING)
- [ ] Start infrastructure services (PostgreSQL, Keycloak, Redis, Kafka)
- [ ] Deploy microservices using Docker Compose
- [ ] Start monitoring stack (Prometheus, Grafana, Jaeger)
- [ ] Deploy Angular frontend

### 🚧 Phase 5: Testing & Verification (PENDING)
- [ ] Test API Gateway endpoints
- [ ] Verify Keycloak authentication
- [ ] Check monitoring dashboards
- [ ] Test frontend application

---

## 🔧 Quick Commands Reference

### SSH Access
```bash
# Gateway Server (Mumbai)
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@13.232.110.186

# Services Server (Mumbai)  
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@43.205.233.120

# Jenkins Server (US East - when deployed)
ssh -i aws-deployment/keypair/terraform-user-us-east.pem ec2-user@<JENKINS_IP>
```

### Deploy Jenkins (Next Step)
```bash
cd aws-deployment/terraform/jenkins-separate
~/bin/terraform.exe init
~/bin/terraform.exe apply
```

### Start Application Services
```bash
# On Gateway Server
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@13.232.110.186
cd /opt/ecommerce/spring-micro-ecommerce-platform
sudo docker-compose -f docker-compose.infrastructure.yml up -d

# On Services Server  
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@43.205.233.120
cd /opt/ecommerce/spring-micro-ecommerce-platform
sudo docker-compose -f docker-compose.services.yml up -d
```

---

## 🌐 Expected Service URLs (After Full Deployment)

| Service | URL | Status |
|---------|-----|--------|
| API Gateway | http://13.232.110.186:8081 | 🚧 Pending |
| Keycloak | http://13.232.110.186:8080 | 🚧 Pending |
| Angular Frontend | http://13.232.110.186:4200 | 🚧 Pending |
| Grafana | http://13.232.110.186:3000 | 🚧 Pending |
| Prometheus | http://13.232.110.186:9090 | 🚧 Pending |
| Jaeger | http://13.232.110.186:16686 | 🚧 Pending |
| Jenkins | http://JENKINS_IP:8080 | 🚧 Pending |

---

## ⚠️ Known Issues & Solutions

### Issue 1: Jenkins Deployment Failed
- **Problem**: AWS Free Tier allows only 2 EC2 instances per region
- **Solution**: Deploy Jenkins in us-east-1 region with separate Terraform config
- **Files Ready**: `aws-deployment/terraform/jenkins-separate/`

### Issue 2: Package Installation Errors
- **Problem**: Some YUM package conflicts during Git installation
- **Status**: Resolved - Git successfully installed on both servers

### Issue 3: Instance Type Restrictions
- **Problem**: c7i-flex.large and t3.medium not free tier eligible
- **Solution**: Using t3.medium for better performance (~$30/month vs free)

---

## 💰 Current Cost Estimate
- Gateway Server (t3.medium): ~$30/month
- Services Server (c7i-flex.large): ~$65/month  
- Jenkins Server (t2.micro): ~$0/month (Free Tier)
- **Total**: ~$95/month

---

## 🎯 Next Steps Priority

1. **HIGH**: Complete Jenkins deployment in us-east-1
2. **HIGH**: Start application services with Docker Compose  
3. **MEDIUM**: Test and verify all service endpoints
4. **LOW**: Set up monitoring dashboards and alerts

---

## 📞 Support Commands

### Check Server Status
```bash
# Check if servers are running
"/c/Program Files/Amazon/AWSCLIV2/aws.exe" ec2 describe-instances \
  --instance-ids i-0923c080b3c95127d i-07dcf8b2b66592293 \
  --query 'Reservations[*].Instances[*].[InstanceId,State.Name,PublicIpAddress]'
```

### Terraform Operations
```bash
cd aws-deployment/terraform
~/bin/terraform.exe plan    # Preview changes
~/bin/terraform.exe apply   # Apply changes  
~/bin/terraform.exe destroy # Cleanup (CAREFUL!)
```

### Docker Status Check
```bash
# Check Docker on both servers
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@13.232.110.186 "sudo systemctl status docker"
ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@43.205.233.120 "sudo systemctl status docker"
```
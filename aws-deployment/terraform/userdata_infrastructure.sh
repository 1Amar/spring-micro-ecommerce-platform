#!/bin/bash

# Infrastructure Server Setup - Ubuntu 22.04 LTS
# Services: Nginx, Keycloak, PostgreSQL, Monitoring (Prometheus, Grafana, Jaeger)

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1
echo "Starting Infrastructure Server setup..."

# Set non-interactive mode for apt
export DEBIAN_FRONTEND=noninteractive

# Update system
apt-get update -y

# Install essential packages
apt-get install -y docker.io docker-compose git wget curl unzip nginx certbot python3-certbot-nginx openjdk-17-jdk

# Start and enable Docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ubuntu

# Create EBS mount point and format
while [ ! -e /dev/nvme1n1 ]; do
  echo "Waiting for EBS volume to be available..."
  sleep 5
done

# Format and mount EBS volume if not already formatted
if ! file -s /dev/nvme1n1 | grep -q filesystem; then
  mkfs -t ext4 /dev/nvme1n1
fi

mkdir -p /mnt/ebs-storage
mount /dev/nvme1n1 /mnt/ebs-storage

# Add to fstab for persistent mounting
echo "/dev/nvme1n1 /mnt/ebs-storage ext4 defaults,nofail 0 2" >> /etc/fstab

# Set permissions
chown ubuntu:ubuntu /mnt/ebs-storage

# Create application directory on EBS storage
mkdir -p /mnt/ebs-storage/ecommerce
chown ubuntu:ubuntu /mnt/ebs-storage/ecommerce

# Create symlink for compatibility
ln -sf /mnt/ebs-storage/ecommerce /opt/ecommerce

# Clone repository
cd /mnt/ebs-storage/ecommerce
sudo -u ubuntu git clone https://github.com/1Amar/spring-micro-ecommerce-platform || echo "Repository clone may have failed"
chown -R ubuntu:ubuntu /mnt/ebs-storage/ecommerce/

# Create Nginx configuration directory
mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled

# Create basic Nginx configuration for domain setup
cat > /etc/nginx/sites-available/${domain_name} << 'EOL'
# Infrastructure Server - API, Auth, Monitoring
server {
    listen 80;
    server_name api.${domain_name} auth.${domain_name} monitor.${domain_name};
    
    # Redirect HTTP to HTTPS (will be configured after SSL setup)
    return 301 https://$server_name$request_uri;
}

# API Gateway (Port 8081)
server {
    listen 443 ssl http2;
    server_name api.${domain_name};
    
    # SSL configuration will be added by certbot
    
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}

# Keycloak (Port 8080)
server {
    listen 443 ssl http2;
    server_name auth.${domain_name};
    
    # SSL configuration will be added by certbot
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # Keycloak specific headers
        proxy_set_header X-Forwarded-Server $host;
        proxy_buffer_size 128k;
        proxy_buffers 4 256k;
        proxy_busy_buffers_size 256k;
    }
}

# Monitoring Dashboard (Grafana on Port 3000)
server {
    listen 443 ssl http2;
    server_name monitor.${domain_name};
    
    # SSL configuration will be added by certbot
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Prometheus endpoint
    location /prometheus {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Jaeger endpoint
    location /jaeger {
        proxy_pass http://localhost:16686;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOL

# Enable the site
ln -sf /etc/nginx/sites-available/${domain_name} /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Update main nginx.conf to include sites-enabled
if ! grep -q "include /etc/nginx/sites-enabled" /etc/nginx/nginx.conf; then
    sed -i '/include \/etc\/nginx\/conf.d\/\*.conf;/a\    include /etc/nginx/sites-enabled/*;' /etc/nginx/nginx.conf
fi

# Test and start Nginx
nginx -t && systemctl start nginx

# Set up environment variables
cat > /etc/environment << EOL
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
PATH=\$PATH:/usr/local/bin
DOMAIN_NAME=${domain_name}
EOL

# Create SSL setup script for later execution
cat > /home/ubuntu/setup-ssl.sh << 'EOL'
#!/bin/bash
echo "Setting up SSL certificates..."

# Wait for DNS propagation
sleep 60

# Get SSL certificates for all domains
sudo certbot --nginx \
  -d api.${domain_name} \
  -d auth.${domain_name} \
  -d monitor.${domain_name} \
  --non-interactive \
  --agree-tos \
  --email admin@${domain_name} \
  --redirect

# Set up auto-renewal
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo tee -a /var/spool/cron/root

# Restart services
sudo systemctl reload nginx

echo "SSL setup completed!"
EOL

chmod +x /home/ubuntu/setup-ssl.sh
chown ubuntu:ubuntu /home/ubuntu/setup-ssl.sh

# Create application startup script
cat > /home/ubuntu/start-infrastructure.sh << 'EOL'
#!/bin/bash
cd /opt/ecommerce/spring-micro-ecommerce-platform

echo "Starting infrastructure services..."

# Start infrastructure services (PostgreSQL, Keycloak, Monitoring)
if [ -f docker-compose.infrastructure.yml ]; then
    sudo docker-compose -f docker-compose.infrastructure.yml up -d
else
    echo "Infrastructure docker-compose file not found!"
fi

echo "Infrastructure services started!"
EOL

chmod +x /home/ubuntu/start-infrastructure.sh
chown ubuntu:ubuntu /home/ubuntu/start-infrastructure.sh

echo "Infrastructure Server setup completed!"
echo "Next steps:"
echo "1. Run: /home/ubuntu/setup-ssl.sh (after DNS propagation)"
echo "2. Run: /home/ubuntu/start-infrastructure.sh (to start services)"
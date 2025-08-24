#!/bin/bash

# Services Server Setup - Ubuntu 22.04 LTS
# Services: All Microservices + Angular Frontend

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1
echo "Starting Services Server setup..."

# Set non-interactive mode for apt
export DEBIAN_FRONTEND=noninteractive

# Update system
apt-get update -y

# Install essential packages
apt-get install -y docker.io docker-compose git wget curl unzip nginx certbot python3-certbot-nginx openjdk-17-jdk maven nodejs npm

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

# Create basic Nginx configuration for frontend app
cat > /etc/nginx/sites-available/${domain_name}-app << 'EOL'
# Services Server - Frontend Application
server {
    listen 80;
    server_name app.${domain_name};
    
    # Redirect HTTP to HTTPS (will be configured after SSL setup)
    return 301 https://$server_name$request_uri;
}

# Angular Frontend Application (Port 4200 or built files)
server {
    listen 443 ssl http2;
    server_name app.${domain_name};
    
    # SSL configuration will be added by certbot
    
    # Serve built Angular files (if available) or proxy to dev server
    root /opt/ecommerce/spring-micro-ecommerce-platform/ecommerce-frontend/dist/ecommerce-frontend;
    index index.html;
    
    # Try to serve static files first, fallback to Angular app
    location / {
        try_files $uri $uri/ @angular;
    }
    
    # Fallback to Angular dev server if static files not available
    location @angular {
        proxy_pass http://localhost:4200;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # API proxy to infrastructure server (for CORS and unified domain)
    location /api/ {
        proxy_pass https://api.${domain_name}/;
        proxy_set_header Host api.${domain_name};
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Auth proxy to infrastructure server
    location /auth/ {
        proxy_pass https://auth.${domain_name}/;
        proxy_set_header Host auth.${domain_name};
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOL

# Enable the site
ln -sf /etc/nginx/sites-available/${domain_name}-app /etc/nginx/sites-enabled/
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
echo "Setting up SSL certificates for app domain..."

# Wait for DNS propagation
sleep 60

# Get SSL certificate for app domain
sudo certbot --nginx \
  -d app.${domain_name} \
  --non-interactive \
  --agree-tos \
  --email admin@${domain_name} \
  --redirect

# Set up auto-renewal
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo tee -a /var/spool/cron/root

# Restart services
sudo systemctl reload nginx

echo "SSL setup completed for Services server!"
EOL

chmod +x /home/ubuntu/setup-ssl.sh
chown ubuntu:ubuntu /home/ubuntu/setup-ssl.sh

# Create application startup script
cat > /home/ubuntu/start-services.sh << 'EOL'
#!/bin/bash
cd /opt/ecommerce/spring-micro-ecommerce-platform

echo "Starting microservices..."

# Start all microservices
if [ -f docker-compose.services.yml ]; then
    sudo docker-compose -f docker-compose.services.yml up -d
else
    echo "Services docker-compose file not found!"
fi

# Build and start Angular frontend if needed
if [ -d "ecommerce-frontend" ]; then
    echo "Setting up Angular frontend..."
    cd ecommerce-frontend
    
    # Install dependencies
    sudo -u ubuntu npm install
    
    # Build for production
    sudo -u ubuntu npm run build
    
    # Copy built files to nginx directory
    if [ -d "dist/ecommerce-frontend" ]; then
        sudo cp -r dist/ecommerce-frontend/* /var/www/html/
    fi
    
    # Or start development server (comment out if using built files)
    # sudo -u ubuntu nohup npm start > /var/log/angular.log 2>&1 &
fi

echo "Services started!"
EOL

chmod +x /home/ubuntu/start-services.sh
chown ubuntu:ubuntu /home/ubuntu/start-services.sh

# Create build script for microservices
cat > /home/ubuntu/build-services.sh << 'EOL'
#!/bin/bash
cd /opt/ecommerce/spring-micro-ecommerce-platform

echo "Building microservices with Maven..."

# Build all services
if [ -f pom.xml ]; then
    sudo -u ubuntu mvn clean package -DskipTests
fi

echo "Build completed!"
EOL

chmod +x /home/ubuntu/build-services.sh
chown ubuntu:ubuntu /home/ubuntu/build-services.sh

echo "Services Server setup completed!"
echo "Next steps:"
echo "1. Run: /home/ubuntu/build-services.sh (to build microservices)"
echo "2. Run: /home/ubuntu/setup-ssl.sh (after DNS propagation)"
echo "3. Run: /home/ubuntu/start-services.sh (to start all services)"
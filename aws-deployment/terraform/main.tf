terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Configure the AWS Provider
provider "aws" {
  region = var.aws_region
}

# Data source for existing key pair
data "aws_key_pair" "existing" {
  key_name = "ecommerce-key"
}

# Get latest Ubuntu 22.04 LTS AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Get availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

# VPC
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.environment}-vpc"
    Environment = var.environment
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.environment}-igw"
    Environment = var.environment
  }
}

# Public Subnet
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.environment}-public-subnet"
    Environment = var.environment
  }
}

# Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "${var.environment}-public-rt"
    Environment = var.environment
  }
}

# Route Table Association
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Security Group for Infrastructure Server (Nginx, Keycloak, etc.)
resource "aws_security_group" "infrastructure" {
  name        = "${var.environment}-infrastructure-sg"
  description = "Security group for infrastructure server"
  vpc_id      = aws_vpc.main.id

  # SSH access
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ip]
    description = "SSH access"
  }

  # HTTP for Let's Encrypt verification
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP for SSL verification"
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS access"
  }

  # Internal communication
  ingress {
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
    description = "Internal VPC communication"
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name        = "${var.environment}-infrastructure-sg"
    Environment = var.environment
  }
}

# Security Group for Services Server
resource "aws_security_group" "services" {
  name        = "${var.environment}-services-sg"
  description = "Security group for services server"
  vpc_id      = aws_vpc.main.id

  # SSH access
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ip]
    description = "SSH access"
  }

  # HTTP for Let's Encrypt verification
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP for SSL verification"
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS access"
  }

  # Internal communication
  ingress {
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
    description = "Internal VPC communication"
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name        = "${var.environment}-services-sg"
    Environment = var.environment
  }
}

# Elastic IP for Infrastructure Server
resource "aws_eip" "infrastructure" {
  domain = "vpc"
  
  tags = {
    Name        = "${var.environment}-infrastructure-eip"
    Environment = var.environment
  }
}

# Elastic IP for Services Server  
resource "aws_eip" "services" {
  domain = "vpc"
  
  tags = {
    Name        = "${var.environment}-services-eip"
    Environment = var.environment
  }
}

# EBS Volume for Infrastructure Server
resource "aws_ebs_volume" "infrastructure" {
  availability_zone = data.aws_availability_zones.available.names[0]
  size              = var.infrastructure_ebs_size
  type              = "gp3"
  
  tags = {
    Name        = "${var.environment}-infrastructure-ebs"
    Environment = var.environment
  }
}

# EC2 Instance - Infrastructure Server
resource "aws_instance" "infrastructure" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.infrastructure_instance_type
  key_name               = data.aws_key_pair.existing.key_name
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.infrastructure.id]

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = base64encode(templatefile("${path.module}/userdata_infrastructure.sh", {
    domain_name = var.domain_name
  }))

  tags = {
    Name        = "${var.environment}-infrastructure-server"
    Environment = var.environment
    Role        = "infrastructure"
  }
}

# EBS Volume Attachment for Infrastructure Server
resource "aws_volume_attachment" "infrastructure" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.infrastructure.id
  instance_id = aws_instance.infrastructure.id
}

# EBS Volume for Services Server
resource "aws_ebs_volume" "services" {
  availability_zone = data.aws_availability_zones.available.names[0]
  size              = var.services_ebs_size
  type              = "gp3"
  
  tags = {
    Name        = "${var.environment}-services-ebs"
    Environment = var.environment
  }
}

# EC2 Instance - Services Server
resource "aws_instance" "services" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.services_instance_type
  key_name               = data.aws_key_pair.existing.key_name
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.services.id]

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = base64encode(templatefile("${path.module}/userdata_services.sh", {
    domain_name = var.domain_name
  }))

  tags = {
    Name        = "${var.environment}-services-server"
    Environment = var.environment
    Role        = "services"
  }
}

# EBS Volume Attachment for Services Server
resource "aws_volume_attachment" "services" {
  device_name = "/dev/sdg"
  volume_id   = aws_ebs_volume.services.id
  instance_id = aws_instance.services.id
}

# Associate Elastic IP with Infrastructure Server
resource "aws_eip_association" "infrastructure" {
  instance_id   = aws_instance.infrastructure.id
  allocation_id = aws_eip.infrastructure.id
}

# Associate Elastic IP with Services Server
resource "aws_eip_association" "services" {
  instance_id   = aws_instance.services.id
  allocation_id = aws_eip.services.id
}

# Route 53 Records
data "aws_route53_zone" "main" {
  count = var.create_dns_records ? 1 : 0
  name  = var.domain_name
}

# A record for API (Infrastructure server)
resource "aws_route53_record" "api" {
  count   = var.create_dns_records ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "api.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.infrastructure.public_ip]
}

# A record for Auth/Keycloak (Infrastructure server)
resource "aws_route53_record" "auth" {
  count   = var.create_dns_records ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "auth.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.infrastructure.public_ip]
}

# A record for App/Frontend (Services server)
resource "aws_route53_record" "app" {
  count   = var.create_dns_records ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "app.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.services.public_ip]
}

# A record for Monitor/Grafana (Infrastructure server)
resource "aws_route53_record" "monitor" {
  count   = var.create_dns_records ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "monitor.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.infrastructure.public_ip]
}
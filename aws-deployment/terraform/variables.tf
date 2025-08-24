variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "domain_name" {
  description = "Domain name for the application"
  type        = string
  default     = "amars.shop"
}

variable "admin_ip" {
  description = "IP address for admin access (SSH)"
  type        = string
  default     = "0.0.0.0/0"  # Change to your IP for better security
}

variable "infrastructure_instance_type" {
  description = "Instance type for infrastructure server"
  type        = string
  default     = "c7i-flex.large"  # 2 vCPU, 4GB RAM - Cost-effective
}

variable "services_instance_type" {
  description = "Instance type for services server"  
  type        = string
  default     = "c7i-flex.large"  # 2 vCPU, 4GB RAM - Cost-effective
}

variable "create_dns_records" {
  description = "Whether to create Route 53 DNS records"
  type        = bool
  default     = true
}

variable "github_repo" {
  description = "GitHub repository URL"
  type        = string
  default     = "https://github.com/1Amar/spring-micro-ecommerce-platform"
}

variable "infrastructure_ebs_size" {
  description = "EBS volume size for infrastructure server (GB)"
  type        = number
  default     = 30
}

variable "services_ebs_size" {
  description = "EBS volume size for services server (GB)"
  type        = number
  default     = 40
}
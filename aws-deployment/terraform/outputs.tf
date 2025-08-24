output "infrastructure_public_ip" {
  description = "Static public IP of the Infrastructure server"
  value       = aws_eip.infrastructure.public_ip
}

output "infrastructure_public_dns" {
  description = "Public DNS of the Infrastructure server"
  value       = aws_instance.infrastructure.public_dns
}

output "services_public_ip" {
  description = "Static public IP of the Services server"
  value       = aws_eip.services.public_ip
}

output "services_public_dns" {
  description = "Public DNS of the Services server"
  value       = aws_instance.services.public_dns
}

output "ssh_commands" {
  description = "SSH commands to connect to servers"
  value = {
    infrastructure = "ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@${aws_eip.infrastructure.public_ip}"
    services      = "ssh -i aws-deployment/keypair/ecommerce-key.pem ec2-user@${aws_eip.services.public_ip}"
  }
}

output "application_urls" {
  description = "Application URLs with domain names"
  value = {
    api_gateway = "https://api.${var.domain_name}"
    auth_keycloak = "https://auth.${var.domain_name}"
    frontend_app = "https://app.${var.domain_name}"
    monitoring = "https://monitor.${var.domain_name}"
  }
}

output "dns_records_created" {
  description = "DNS records that will be created"
  value = var.create_dns_records ? [
    "api.${var.domain_name} → ${aws_eip.infrastructure.public_ip}",
    "auth.${var.domain_name} → ${aws_eip.infrastructure.public_ip}",
    "app.${var.domain_name} → ${aws_eip.services.public_ip}",
    "monitor.${var.domain_name} → ${aws_eip.infrastructure.public_ip}"
  ] : ["DNS records creation disabled"]
}

output "cost_estimate" {
  description = "Monthly cost estimate"
  value = "~$120-150/month (2x Elastic IPs: $7.30, t3.medium: $35, t3.large: $70, Route53: $0.50)"
}

output "setup_summary" {
  description = "Deployment summary"
  value = {
    infrastructure_server = {
      ip = aws_eip.infrastructure.public_ip
      role = "Nginx, Keycloak, PostgreSQL, Monitoring"
      domains = ["api.${var.domain_name}", "auth.${var.domain_name}", "monitor.${var.domain_name}"]
    }
    services_server = {
      ip = aws_eip.services.public_ip
      role = "Microservices, Angular Frontend"
      domains = ["app.${var.domain_name}"]
    }
  }
}
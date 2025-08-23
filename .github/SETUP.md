# GitHub Actions Setup

## Required Repository Secret

Add this secret to your GitHub repository:

1. Go to: `https://github.com/1Amar/spring-micro-ecommerce-platform/settings/secrets/actions`
2. Click "New repository secret"
3. **Name**: `EC2_PRIVATE_KEY`
4. **Value**: Copy the content of your EC2 private key file
5. Click "Add secret"

## Deployment

The workflow will automatically deploy when you push to the `main` branch.

## Current Infrastructure

- **Gateway Server**: 13.235.42.64
- **Services Server**: 3.111.144.135

## Services URLs

- **Grafana**: http://13.235.42.64:3000 (admin/admin)
- **Prometheus**: http://13.235.42.64:9090
- **Jaeger**: http://13.235.42.64:16686
- **Keycloak**: http://13.235.42.64:8080
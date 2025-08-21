# Spring Micro-Ecommerce Platform

This document provides an overview and instructions for setting up and running the Spring Micro-Ecommerce Platform.

## Project Overview

This project is a microservices-based e-commerce platform built using Spring Boot and Spring Cloud. It consists of several independent services that communicate with each other to provide a complete e-commerce solution.

## Services

- **catalog-service**: Manages product catalog.
- **common-library**: Common utilities and models shared across services.
- **ecom-api-gateway**: API Gateway for routing requests to various services.
- **ecom-order-service**: Handles order processing.
- **eureka-service-registry**: Service registry for microservices.
- **inventory-service**: Manages product inventory.
- **notification-service**: Sends notifications.
- **notification-worker**: Processes notification tasks.
- **payment-service**: Handles payment processing.
- **product-service**: Manages product details.
- **search-service**: Provides search functionality.

## Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8.x or higher
- Docker and Docker Compose (for local infrastructure)

### Building the Project

To build all microservices, navigate to the root directory of the project and run:

```bash
mvn clean install
```

## Running the Services

### 1. Start Infrastructure (Docker)

Navigate to the `Docker` directory and start the required infrastructure services (e.g., databases, message brokers) using Docker Compose:

```bash
cd Docker
docker-compose up -d
```

### 2. Start Eureka Service Registry

Start the Eureka Service Registry first, as other services will register with it.

```bash
cd eureka-service-registry
mvn spring-boot:run
```

### 3. Start Other Services

Start other microservices. You can run them individually in separate terminals or use your IDE.

Example:

```bash
cd catalog-service
mvn spring-boot:run
```

Repeat for `ecom-api-gateway`, `ecom-order-service`, `inventory-service`, `notification-service`, `notification-worker`, `payment-service`, `product-service`, and `search-service`.

## Common Commands

- **Build all services**: `mvn clean install` (from root)
- **Run a specific service**: `mvn spring-boot:run` (from service directory)
- **Stop Docker containers**: `docker-compose down` (from `Docker` directory)

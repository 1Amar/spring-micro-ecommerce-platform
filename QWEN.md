# Spring Micro E-Commerce Platform - Qwen Context

## Project Overview

This is a microservices-based e-commerce platform built using Java, Spring Boot, and Spring Cloud. It's designed with a modular architecture where each business domain is a separate microservice. The platform uses Maven for build management and incorporates modern cloud-native technologies for service discovery, API gateway, security, observability, and messaging.

### Core Technologies

- **Java 17**: Primary programming language.
- **Spring Boot 3.2.0**: For building standalone, production-grade Spring-based applications.
- **Spring Cloud 2023.0.1**: Provides tools for building distributed systems (e.g., configuration management, service discovery, circuit breakers).
- **Maven**: Build automation and dependency management.
- **Docker & Docker Compose**: For containerization and orchestration of infrastructure services.
- **PostgreSQL**: Primary relational database.
- **Redis**: In-memory data structure store, used for caching.
- **Apache Kafka**: Distributed event streaming platform for asynchronous communication.
- **Elastic Stack (ELK)**: Elasticsearch, Logstash, Kibana for centralized logging and monitoring.
- **Keycloak**: Identity and Access Management (IAM) for authentication and authorization.
- **Jaeger**: Distributed tracing system for monitoring and troubleshooting microservices-based distributed systems.
- **Prometheus & Grafana**: Monitoring and alerting toolkit.
- **Angular 16**: Frontend framework for building the user interface.

### Architecture Components

- **Infrastructure Services** (Managed by Docker Compose):
  - PostgreSQL (`postgres`): Database for services.
  - Apache Kafka (`kafka`, `zookeeper`): Event streaming.
  - Redis (`redis`): Caching.
  - Elastic Stack (`elasticsearch`, `logstash`, `kibana`): Centralized logging.
  - Keycloak (`keycloak`): Authentication and authorization.
  - Jaeger (`jaeger`): Distributed tracing.
  - Prometheus (`prometheus`): Metrics collection.
  - Grafana (`grafana`): Metrics visualization.
- **Core Platform Services**:
  - `common-library`: A shared library containing common dependencies for observability (Micrometer, OpenTelemetry), logging (Logstash encoder), and security (Spring Security, OAuth2 Resource Server).
  - `eureka-service-registry`: Netflix Eureka server for service discovery.
  - `ecom-api-gateway`: Spring Cloud Gateway for routing, securing, and monitoring API requests.
  - Individual Business Microservices:
    - `product-service`: Manages product information.
    - `inventory-service`: Manages stock levels.
    - `catalog-service`: Aggregates product and inventory data.
    - `ecom-order-service`: Handles order processing.
    - `payment-service`: Manages payment transactions.
    - `notification-service`: Manages notification requests.
    - `notification-worker`: Processes notifications sent via Kafka.
    - `search-service`: Provides search functionality (likely using Elasticsearch).

## Building and Running

### Prerequisites

- Java 17 JDK
- Apache Maven
- Docker & Docker Compose
- Node.js and npm (for the Angular frontend)

### Build Process

1.  **Build Common Library**: The `common-library` must be built first as it's a dependency for other services.
    ```bash
    cd common-library
    mvn clean install
    ```
2.  **Build All Services**: A Windows batch script `build-all-services.cmd` is provided to build all microservices sequentially. It executes `mvn clean compile` for each service.
    ```bash
    build-all-services.cmd
    ```
    *(Note: This script assumes Maven is in your PATH and uses hardcoded paths.)*

### Running the Application

1.  **Start Infrastructure**: Use Docker Compose to start all supporting services (databases, message brokers, monitoring tools).
    ```bash
    cd Docker
    docker-compose up -d
    ```
2.  **Start Core Platform Services**:
    - Start `eureka-service-registry` first.
    - Then start `ecom-api-gateway`.
    - Finally, start the other business microservices in any order.
3.  **Start Frontend**:
    - Navigate to the `ecommerce-frontend` directory.
    - Install dependencies: `npm install`
    - Start the development server: `npm start`
4.  **Access Applications**:
    - Frontend: `http://localhost:4200`
    - Keycloak Admin Console: `http://localhost:8080/admin/` (Credentials: `admin`/`admin`)
    - Eureka Dashboard: `http://localhost:8761` (Default port for Eureka)
    - Jaeger UI: `http://localhost:16686`
    - Kibana: `http://localhost:5601`
    - Prometheus: `http://localhost:9090`
    - Grafana: `http://localhost:3000`

## Development Conventions

### Java / Spring Boot

- Services are structured as Maven projects.
- The root `pom.xml` defines the parent POM and manages shared dependencies (Spring Boot, Spring Cloud versions).
- Each microservice inherits from the root POM and defines its specific dependencies.
- The `common-library` module provides shared configurations for:
  - Observability (Actuator, Micrometer with Prometheus, OpenTelemetry tracing).
  - Structured logging (Logstash encoder).
  - Security (Spring Security, OAuth2 Resource Server, JWT).
- Services are expected to register with Eureka for discovery.
- The API Gateway routes requests to services based on their Eureka-registered names.
- Authentication is handled by Keycloak. Services act as OAuth2 Resource Servers, validating JWT tokens issued by Keycloak.

### Frontend (Angular)

- Uses Angular CLI for scaffolding and building.
- Integrates with Keycloak using `keycloak-angular` and `keycloak-js` libraries for authentication.

### Testing

- Unit and integration tests are included within each service using Spring Boot Test.
- Specific testing guides exist for role-based access (`test-role-access.md`) and observability (`test-observability.md`).

### Security (Keycloak)

- A detailed setup guide (`setup-keycloak.md`) describes how to configure Keycloak with realms, clients (frontend and backend), roles (`admin`, `manager`, `customer`, `support`), and test users.
- Spring services are configured to use Keycloak as the JWT issuer.

## Key Documentation Files

- `README.md`: Basic project description and list of microservices.
- `setup-keycloak.md`: Comprehensive guide for Keycloak configuration.
- `test-role-access.md`: Instructions for testing role-based access control.
- `test-observability.md`: Instructions for testing observability setup (Jaeger, Prometheus, Grafana, ELK).
- `test-opentelemetry.md`: Likely related to testing OpenTelemetry tracing.
- `CLAUDE.md`, `gemini.md`: Context files for other AI assistants.
- `build-all-services.cmd`: Windows script to build all services.
- `setup-frontend.cmd`: Presumably a script to set up the frontend environment.

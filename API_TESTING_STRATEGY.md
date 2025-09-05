# Comprehensive API Testing Strategy

This document outlines the complete API testing strategy for the Spring Boot Microservices E-Commerce Platform.

## 1. Testing Strategy Framework

### Methodologies
- **Testing Pyramid:** Emphasis on a strong foundation of unit tests, a healthy layer of integration tests, and a smaller, focused set of end-to-end tests.
- **Shift-Left Testing:** Integrating testing early in the development cycle. Code reviews, static analysis, and unit tests are mandatory before merging.
- **Continuous Testing:** Every commit triggers a build and executes automated unit and integration tests in a CI/CD pipeline.

### Recommended Toolchain
- **Unit Testing:** JUnit 5, Mockito, and `spring-boot-starter-test`.
- **Integration & E2E Testing:** REST Assured (Java-based) for API automation, providing a BDD-style syntax. Postman/Newman for exploratory testing and simple CI integration.
- **Performance Testing:** Apache JMeter or Gatling.
- **CI/CD:** Jenkins or GitHub Actions.

### Testing Environments
- **Local:** Developers run unit and integration tests locally.
- **Dev/Test:** A dedicated, scaled-down environment where all microservices and infrastructure (PostgreSQL, Redis, Kafka) are deployed. This environment is used for automated integration and E2E test runs.
- **Staging:** A production-like environment for performance and final pre-release validation.

---

## 2. Individual Service Test Plans

### User Service (Port 8082)
- **`POST /auth/token` (Keycloak)**
  - **TC-US-01 (Happy Path):** Request token with valid username/password. **Expected:** `200 OK` with JWT access and refresh tokens.
  - **TC-US-02 (Failure):** Request token with invalid credentials. **Expected:** `401 Unauthorized`.
- **`GET /users/profile`**
  - **TC-US-03 (Happy Path):** Get user profile with a valid JWT. **Expected:** `200 OK` with user profile data.
  - **TC-US-04 (Auth Failure):** Get user profile with an invalid or expired JWT. **Expected:** `401 Unauthorized`.
- **`PUT /users/profile`**
  - **TC-US-05 (Happy Path):** Update user profile with valid data and JWT. **Expected:** `200 OK` and subsequent `GET` reflects the changes.

### Product Service (Port 8088)
- **`GET /api/products`**
  - **TC-PS-01 (Happy Path):** Fetch the first page of products. **Expected:** `200 OK` with a paginated list of products.
- **`GET /api/products/{id}`**
  - **TC-PS-02 (Happy Path):** Fetch an existing product by its ID. **Expected:** `200 OK` with product details.
  - **TC-PS-03 (Not Found):** Fetch a non-existent product ID. **Expected:** `404 Not Found`.
- **`GET /api/products/search`**
  - **TC-PS-04 (Happy Path):** Search for products using a keyword. **Expected:** `200 OK` with relevant results.

### Cart Service (Port 8089)
- **`GET /api/cart`**
  - **TC-CS-01 (Happy Path):** Get the current user's cart. **Expected:** `200 OK` with cart contents.
- **`POST /api/cart/items`**
  - **TC-CS-02 (Happy Path):** Add a valid product to the cart. **Expected:** `201 Created`.
  - **TC-CS-03 (Bad Request):** Add a product with quantity zero or negative. **Expected:** `400 Bad Request`.
- **`DELETE /api/cart/items/{itemId}`**
  - **TC-CS-04 (Happy Path):** Remove an item from the cart. **Expected:** `204 No Content`.
- **`POST /api/cart/clear`**
  - **TC-CS-05 (Happy Path):** Clear the entire cart. **Expected:** `204 No Content`.
- **TC-CS-06 (Persistence):** Add item to cart, simulate session expiry, re-authenticate. **Expected:** Cart contents remain intact in Redis.

### Inventory Service (Port 8084)
*(Note: These are often internal endpoints called by other services)*
- **`GET /api/inventory/{sku}`**
  - **TC-IS-01 (Happy Path):** Check stock for a product. **Expected:** `200 OK` with quantity.
- **Internal Kafka Event Listeners:**
  - **TC-IS-02 (Reservation):** Order Service requests reservation. **Expected:** Inventory quantity is reduced, and a reservation entry is created.
  - **TC-IS-03 (Commit):** Payment Service confirms payment. **Expected:** Reservation is removed, and stock is permanently committed.
  - **TC-IS-04 (Release):** Order fails post-reservation. **Expected:** Reservation is removed, and inventory quantity is restored.

### Order Service (Port 8083)
- **`POST /api/orders`**
  - **TC-OS-01 (Happy Path):** Create an order from a non-empty cart with items in stock. **Expected:** `201 Created` with order details.
  - **TC-OS-02 (Failure):** Create an order when cart is empty. **Expected:** `400 Bad Request`.
  - **TC-OS-03 (Out of Stock):** Create an order for an item that is out of stock. **Expected:** `409 Conflict` or similar error indicating stock issue.
- **`GET /api/orders/{orderId}`**
  - **TC-OS-04 (Happy Path):** Fetch an existing order. **Expected:** `200 OK`.
  - **TC-OS-05 (Forbidden):** User A tries to fetch User B's order. **Expected:** `403 Forbidden`.

---

## 3. Integration Testing Matrix

| Calling Service      | Called Service / System | Interaction Point                               | Test Scenario                                                                 |
| -------------------- | ----------------------- | ----------------------------------------------- | ----------------------------------------------------------------------------- |
| **API Gateway**      | **User Service**        | `/auth/token`                                   | Route login requests and return JWT.                                          |
| **API Gateway**      | **Any Service**         | Any protected endpoint                          | Validate valid/invalid/expired JWTs and enforce security policies.            |
| **Order Service**    | **Cart Service**        | `GET /api/cart`                                 | On order creation, fetch the user's current cart.                            |
| **Order Service**    | **Inventory Service**   | Kafka Topic: `inventory.reservation`            | Publish a reservation event when an order is placed.                          |
| **Order Service**    | **Payment Service**     | REST Call: `POST /api/payments`                 | After successful reservation, trigger payment processing.                     |
| **Payment Service**  | **Order Service**       | Kafka Topic: `payment.success` / `payment.failure` | Publish payment outcome to notify the order service.                          |
| **Order Service**    | **Inventory Service**   | Kafka Topic: `inventory.commit` / `inventory.release` | Based on payment outcome, publish event to commit stock or release reservation. |
| **Inventory Service**| **Kafka**               | Publishes `stock.updated` event                 | Verify that stock changes (commit/release) publish an event for other listeners. |

---

## 4. End-to-End Scenarios

### E2E-01: Successful Order Placement (Happy Path)
1.  **Login:** User authenticates via `User Service` and obtains a JWT.
2.  **Add to Cart:** User adds one or more available products to their cart (`Product Service` -> `Cart Service`).
3.  **Create Order:** User initiates checkout. `Order Service` reads the cart.
4.  **Reserve Inventory:** `Order Service` publishes a `reserve-stock` event to Kafka. `Inventory Service` consumes it and reserves the items.
5.  **Process Payment:** `Order Service` calls `Payment Service`. The payment is successful.
6.  **Commit Stock:** `Payment Service` publishes a `payment-successful` event. `Order Service` consumes it and publishes a `commit-stock` event. `Inventory Service` consumes this and decrements the stock permanently.
7.  **Confirm Order:** `Order Service` marks the order as `CONFIRMED`.
8.  **Verification:** User can retrieve the order details and see its status as `CONFIRMED`. Inventory count for the product is verifiably reduced.

---

## 5. Error Handling & Rollback Tests

### ERR-01: Payment Failure Rollback
- **Scenario:** Follow E2E-01 steps 1-4. At step 5, the payment fails (e.g., insufficient funds).
- **Expected Outcome:**
  - `Payment Service` returns a failure or publishes a `payment-failure` event.
  - `Order Service` marks the order as `FAILED`.
  - `Order Service` publishes a `release-stock` event.
  - `Inventory Service` consumes the event and releases the reservation, restoring the previous stock count.
  - The user's cart is NOT cleared.

### ERR-02: Inventory Reservation Failure
- **Scenario:** User tries to order a product where requested quantity > available stock.
- **Expected Outcome:**
  - `Order Service` publishes `reserve-stock`.
  - `Inventory Service` fails to reserve and publishes a `reservation-failed` event.
  - `Order Service` consumes the event, marks the order as `FAILED`, and returns an error to the user (e.g., `409 Conflict`).

### ERR-03: Service Unavailability (Circuit Breaker)
- **Scenario:** `Order Service` attempts to call `Payment Service`, but it is down.
- **Expected Outcome:**
  - The circuit breaker in `Order Service` opens after a few failed attempts.
  - Subsequent requests fail fast, immediately returning an error without waiting for a timeout.
  - The system returns a graceful error message (e.g., `503 Service Unavailable`).
  - Once the `Payment Service` is back online, the circuit breaker closes, and operations resume.

---

## 6. Performance Benchmarks

### General
- **Target Response Time:** p95 latency < 500ms for all `GET` requests.
- **Target Error Rate:** < 0.1% under peak load.

### Scenarios
- **PERF-01 (Read Heavy):** Simulate 1000 concurrent users browsing products (`Product Service`) and checking their carts (`Cart Service`).
- **PERF-02 (Write Heavy - Flash Sale):** Simulate 500 concurrent users adding items to the cart and creating orders within a 5-minute window.
- **PERF-03 (Stress Test):** Gradually increase load until response times degrade significantly or services start failing. Identify the bottleneck.

### Metrics to Monitor
- Service CPU and Memory Utilization.
- API Gateway latency.
- Database connection pool saturation.
- Kafka consumer lag.

---

## 7. Security Testing

### SEC-01: Authentication Bypass
- **Test:** Attempt to access any protected endpoint (e.g., `GET /api/orders/{id}`) without a JWT.
- **Expected:** `401 Unauthorized`.

### SEC-02: Invalid/Expired Token
- **Test:** Access a protected endpoint with a JWT that is expired, has an invalid signature, or is malformed.
- **Expected:** `401 Unauthorized`.

### SEC-03: Cross-User Data Access (Authorization)
- **Test:** User A authenticates. Using User A's JWT, attempt to access User B's data (e.g., `GET /api/cart` but with User B's session identifier, or `GET /api/orders/{orderIdForUserB}`).
- **Expected:** `403 Forbidden`.

### SEC-04: Role-Based Access Control (RBAC)
- **Test:** If admin roles exist, authenticate as a regular user and attempt to access an admin-only endpoint.
- **Expected:** `403 Forbidden`.

---

## 8. Test Data Management

### Strategy
- **Seeding:** Use a combination of SQL scripts and application-level data loaders to populate test data before a test run. This should be automated via a script.
- **Isolation:** Tests must be atomic and independent. A test should create the specific data it needs and, if possible, clean up after itself.
- **Cleanup:** For CI/CD environments, the entire database schema should be dropped and recreated at the start of each full test execution to ensure a clean slate.
- **Data Sets:**
  - **Users:** A pool of test users with known credentials and roles (e.g., `testuser1`, `adminuser`).
  - **Products:** A catalog of ~50 products with known IDs, prices, and initial stock levels. Include products that are out-of-stock or have low stock.
  - **Carts/Orders:** These are typically created dynamically during tests.

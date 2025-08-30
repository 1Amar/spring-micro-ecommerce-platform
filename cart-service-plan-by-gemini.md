Architectural Analysis & Recommendations

  1. Storage Architecture: Redis-First, PostgreSQL-Backed

  The core of this design is a cache-aside pattern with write-behind persistence. Redis will serve as the primary, low-latency store for all active cart operations to meet the sub-50ms
  goal. PostgreSQL will act as the durable backup (the "source of truth") for authenticated users' carts.

   * How to handle dual storage?
       * Read Path: All reads (for both anonymous and authenticated users) must go to Redis first.
           1. Client requests cart.
           2. Cart Service attempts to fetch from Redis using the appropriate key.
           3. Cache Hit: Return the cart data from Redis immediately.
           4. Cache Miss (Authenticated User): The cart is inactive or has been evicted. Fetch the cart from the PostgreSQL persistent_carts and cart_items tables.
           5. Load the retrieved cart data back into Redis with a new TTL (cache warming).
           6. Return the cart data.
       * Write Path (Write-Behind): All writes happen in Redis first, then asynchronously persist to PostgreSQL for authenticated users.
           1. Client adds/updates an item.
           2. Cart Service performs the operation atomically in Redis.
           3. The service immediately returns a 200 OK response to the user (<50ms).
           4. For authenticated users, the service publishes a CartUpdated event to a dedicated Kafka topic.
           5. A listener within the same service consumes this event and saves the state to PostgreSQL. This decouples the user-facing request from database latency.

   * Optimal Redis Key Structure:
       * Authenticated Cart: cart:auth:{userId} (e.g., cart:auth:1a2b3c-4d5e...)
       * Anonymous Cart: cart:anon:{sessionId} (e.g., cart:anon:a8f5b12-e34d...)
       * Data Structure: Use a Redis Hash. This is ideal for modeling a cart, as you can add, remove, and update item quantities atomically.
           * HSET cart:auth:123 product:9876 '{"quantity": 2, "price": 49.99, "addedAt": "..."}'
           * HINCRBY cart:auth:123 product:9876_quantity 1 (Alternative for quantity-only updates)

   * Fallback Strategy (Redis Unavailability):
       * If the Redis connection is lost, the service should degrade gracefully.
       * The service will bypass Redis and fall back to reading/writing directly from/to PostgreSQL for authenticated users.
       * Operations for anonymous users will fail, as they have no persistent store. This is an acceptable trade-off, as a Redis outage is a critical infrastructure failure.
       * Implement a Circuit Breaker (using Resilience4j) on the Redis connection to manage this transition smoothly.

  2. Session vs. Authenticated Cart Management

   * Identifying Anonymous Carts:
       * Upon the first interaction with the cart service from an unknown user, generate a cryptographically secure UUID for the sessionId.
       * Return this sessionId to the client in a secure, HttpOnly cookie with a 15-day expiry. The client must send this cookie with every subsequent request.

   * Cart Merging (Anonymous → Authenticated):
       1. User logs in. The frontend makes a request to a dedicated endpoint, e.g., POST /api/v1/cart/merge.
       2. The request body must contain the userId (from the auth token) and the sessionId (from the cookie).
       3. The Cart Service fetches the anonymous cart from Redis (cart:anon:{sessionId}).
       4. It fetches the authenticated user's existing cart (cart:auth:{userId}), if any.
       5. It intelligently merges the anonymous cart's items into the authenticated cart. For conflicts (same product), the quantity from the anonymous cart is typically added to the
          authenticated one.
       6. The merged cart is saved to Redis under cart:auth:{userId}.
       7. An async CartUpdated event is published to persist the merged cart to PostgreSQL.
       8. The old anonymous cart (cart:anon:{sessionId}) is deleted from Redis.

   * Cart Expiration and Cleanup:
       * Anonymous Carts: Leverage Redis's native TTL. Set a 15-day TTL on all cart:anon:* keys. Redis will handle the cleanup automatically.
       * Authenticated Carts: These are persistent in PostgreSQL. The Redis representation (cart:auth:*) can have a shorter, sliding TTL (e.g., 30 days). If the user is inactive for 30 days,
         the Redis copy expires but the data remains safe in PostgreSQL, ready to be re-hydrated.

  3. Performance & Concurrency

   * Handling Concurrent Updates (Race Conditions):
       * Avoid application-level locking. Rely on Redis's atomic commands.
       * Use HSET to add/update items and HDEL to remove them. These operations are atomic per-key.
       * For quantity updates, HINCRBY is atomic and perfect for this use case, preventing race conditions where two requests try to update the quantity simultaneously.

   * Optimistic vs. Pessimistic Locking:
       * Neither is ideal here. Pessimistic locking is too slow. Optimistic locking (using WATCH/MULTI/EXEC) adds complexity.
       * Recommended: Stick to atomic single-hash operations. If a more complex transaction is needed (e.g., add item A only if item B exists), use a Lua script executed with EVAL. Lua
         scripts are executed atomically on the Redis server, providing a powerful and fast transaction mechanism.

   * Caching for Product Validation:
       * Do not call the Product Service on every cart read.
       * When an item is added to the cart, make a synchronous call to the Product Service to get the current price and availability.
       * Store this validated price inside the Redis cart hash alongside the product ID and quantity.
       * To reduce load on the Product Service, the Cart Service should maintain an in-memory Caffeine cache for product details with a short TTL (e.g., 1-5 minutes).

   * Async vs. Sync Persistence:
       * Asynchronous is mandatory for meeting the performance target. The write-behind pattern using Kafka is the recommended approach. The small risk of data loss (if the service and Redis
         crash between the write and the Kafka message being sent) is an acceptable trade-off for a shopping cart's performance.

  4. Data Consistency & Validation

   * Handling Price/Availability Changes:
       * The price is snapshotted when the item is added. The cart should display this price.
       * Crucial: The Order Service (during checkout) is the final authority. It must re-validate the price and availability of every cart item against the Product Service before creating an
         order.
       * If a price has changed or a product is unavailable, the checkout process must be halted, and the user must be clearly notified to approve the changes or be informed of the removal.

   * Maintaining Data Integrity:
       * The write-behind mechanism ensures that PostgreSQL is the durable source of truth. In case of a full Redis wipe, authenticated carts can be fully restored.
       * The Kafka topic for persistence should be configured with a replication factor > 1 to prevent message loss.

   * Validation Timing:
       * Add-to-Cart Time: Validate for immediate user feedback.
       * Checkout Time: Re-validate for transactional integrity. This is non-negotiable.

  5. Event-Driven Architecture (Kafka)

   * Kafka Events to Publish:
       * cart.item.added (payload: userId, sessionId, productId, quantity, price)
       * cart.item.removed (payload: userId, sessionId, productId)
       * cart.item.updated (payload: userId, sessionId, productId, newQuantity)
       * cart.created (payload: userId, sessionId)
       * cart.cleared (payload: userId, sessionId)
       * cart.merged (payload: oldSessionId, newUserId)

   * Cart Abandonment Tracking:
       * A separate service (or a Kafka Streams application) can consume these cart events.
       * If a cart.item.added or cart.item.updated event is not followed by an order.created event for that user within a configurable period (e.g., 3 hours), it can trigger a "cart
         abandoned" event.
       * This event can be consumed by the Notification Service to send a reminder email.

   * Handoff to Order Service:
       * The checkout initiation is a synchronous API call from the client, likely through the API Gateway.
       * The Order Service calls the Cart Service via a REST endpoint (GET /api/v1/cart) to retrieve the final cart state.
       * The Order Service then takes full ownership of the data for the remainder of the checkout process.

   * Inventory Reservation:
       * Do not reserve inventory on cart operations. This would lead to stock being unavailable for legitimate buyers.
       * Inventory reservation is the responsibility of the Order Service and should only occur after the user has initiated the payment step.

  6. Integration Patterns

   * Calling Product/User Services:
       * Use Spring's WebClient (from WebFlux) for non-blocking, asynchronous REST calls to other services.
       * Integrate with Resilience4j to implement:
           * Circuit Breakers: To prevent cascading failures if the Product Service is slow or down.
           * Retries: For transient network errors.
           * TimeLimiters: To enforce timeouts on calls.

   * User Service Integration:
       * The Cart Service should be stateless regarding authentication.
       * The API Gateway is responsible for validating the JWT from Keycloak.
       * The Gateway should then inject the userId from the token into a downstream header (e.g., X-User-Id). The Cart Service can trust this header as it's coming from a trusted internal
         component.

  ---

  Specific Deliverables Summary

   1. Storage Architecture Diagram (Conceptual Flow):

   1     Client ↔ API Gateway ↔ Cart Service
   2                                 │
   3            (Fast Lane) ┌────────┴────────┐ (Graceful Fallback)
   4                        ↓                 ↓
   5                      Redis (Carts)     PostgreSQL (Auth Carts)
   6                        ↑                 ↑ │
   7                        └─────────────────┘ │ (Async Write-Behind)
   8                                  │       ↓
   9                                  └───── Kafka (`CartUpdated` events)

   2. Database Schema Recommendations:

    1     -- For durable storage of authenticated user carts
    2     CREATE TABLE persistent_carts (
    3         id UUID PRIMARY KEY,
    4         user_id VARCHAR(255) NOT NULL UNIQUE, -- From Keycloak
    5         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    6         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    7     );
    8
    9     CREATE TABLE cart_items (
   10         id UUID PRIMARY KEY,
   11         cart_id UUID NOT NULL REFERENCES persistent_carts(id) ON DELETE CASCADE,
   12         product_id BIGINT NOT NULL, -- Assuming product ID is a long
   13         quantity INT NOT NULL CHECK (quantity > 0),
   14         -- Price at the time of adding to cart, for display purposes
   15         unit_price NUMERIC(10, 2) NOT NULL,
   16         added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   17         UNIQUE(cart_id, product_id) -- A product can only appear once per cart
   18     );
   19
   20     -- Indexes for performance
   21     CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
   22     CREATE INDEX idx_persistent_carts_user_id ON persistent_carts(user_id);

   3. Redis Data Structure Recommendations:
       * Key (Anon): cart:anon:{sessionId}
       * Key (Auth): cart:auth:{userId}
       * Type: HASH
       * Fields:
           * product:{productId} -> Value: JSON String '{"q": 2, "p": 99.99, "a": "timestamp"}' (q=quantity, p=price, a=addedAt)
       * TTL (Anon): 15 days (EXPIRE cart:anon:{sessionId} 1296000)
       * TTL (Auth): 30 days, sliding (EXPIRE cart:auth:{userId} 2592000)

   4. Error Handling & Edge Cases:
       * Redis Down: Fallback to PostgreSQL for authenticated users; fail for anonymous.
       * Product Service Down: Fail the operation (add/update) with a 503 Service Unavailable and a clear message.
       * Merge Conflict: Default strategy is to merge quantities. More complex logic can be added if required by business.
       * Stale Data in Cart: Handled by mandatory re-validation at checkout time.

   5. Performance Optimization Summary:
       * Redis-first, read-heavy design.
       * Write-behind persistence via Kafka to decouple from DB latency.
       * Use of atomic Redis operations (HSET, HINCRBY).
       * In-memory Caffeine cache for short-lived product data.
       * Non-blocking I/O with WebClient.

  This architecture provides a robust, scalable, and high-performance foundation for the Cart Service that aligns perfectly with the existing microservices ecosystem and is ready for
  future integration with the Order Service.
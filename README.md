## Distributed Rate Limiter as a Service
A high-performance distributed rate limiter built using Spring Boot, Redis (Lua scripting), and PostgreSQL.


### Architecture

- **Stateless application**:
  - No in-memory rate limit state is kept in the application.
  - All rate limiting state lives in **Redis**; configuration and users live in **PostgreSQL**.
- **Token Bucket algorithm**:
  - Implemented via a **Redis Lua script** to guarantee atomic refill + token consumption.
  - Bucket state is stored per `userId + endpoint` in Redis.
  - Capacity and refill rate are derived from the user's **plan** and endpoint.
- **Data model (PostgreSQL)**:
  - `users (user_id, plan)` – `plan` is `NORMAL` or `PREMIUM`.
  - `plans (plan)` – two rows only: `NORMAL`, `PREMIUM`.
  - `plan_rate_limits (plan, endpoint, capacity, refill_rate)`:
    - `plan` – `NORMAL` or `PREMIUM`.
    - `endpoint` – logical endpoint identifier (string).
    - `capacity` – max tokens in the bucket.
    - `refill_rate` – tokens per minute.
- **Redis usage**:
  - **Token bucket state**:
    - Key: `rate_limiter:{userId}:{endpoint}`.
    - Hash fields:
      - `tokens` – remaining tokens.
      - `last_refill_ts` – last refill timestamp in ms.
  - **Cached rate-limit config**:
    - Key: `plan_rate_limit:{PLAN}:{endpoint}`.
    - Hash fields:
      - `capacity`
      - `refillRate`
- **Layers**:
  - **Controller**:
    - `RateLimitController` – `/api/v1/rate-limit/check`.
    - `AdminPlanRateLimitController` – `/api/v1/admin/plan-rate-limit`.
  - **Service**:
    - `RateLimitService` – orchestrates user lookup, plan resolution, rate-limit lookup, and token bucket call.
    - `PlanRateLimitService` – CRUD + caching of plan-based rate limits.
    - `RedisTokenBucketService` – executes the Lua script in Redis.
  - **Repository**:
    - `UserRepository`, `PlanDefinitionRepository`, `PlanRateLimitRepository` – Spring Data JPA.

### How rate limiting works

1. **Client calls** `POST /api/v1/rate-limit/check` with:
   - `userId`
   - `endpoint`
2. **`RateLimitService`**:
   - Loads the `User` from PostgreSQL (`users` table).
   - Reads `plan` (`NORMAL` or `PREMIUM`).
   - Fetches `PlanRateLimit` for `(plan, endpoint)` from:
     - Redis cache (`plan_rate_limit:{PLAN}:{endpoint}`) first.
     - If miss, loads from PostgreSQL `plan_rate_limits` and writes to Redis.
3. **Lua token bucket** (`scripts/token_bucket.lua`) via `RedisTokenBucketService`:
   - Key: `rate_limiter:{userId}:{endpoint}`.
   - Inputs: `capacity`, `refillRate` (tokens/min), `currentTimeMillis`.
   - Logic:
     - Reads `tokens` and `last_refill_ts` from Redis hash.
     - If missing, initializes `tokens = capacity`, `last_refill_ts = now`.
     - Computes elapsed time in ms and refills tokens:
       - `tokens += floor(elapsedMs * (refillRate / 60000))` (capped at `capacity`).
     - If `tokens > 0`:
       - Decrements `tokens` by 1 and marks **allowed = true**.
     - Else:
       - Marks **allowed = false** and calculates `retryAfterMs` (time until one token).
     - Updates Redis hash and may set an expiry when bucket is full.
4. **Response**:
   - `allowed` – whether the request is within the limit.
   - `remainingTokens` – remaining tokens in the bucket.
   - `retryAfterMs` – milliseconds until a token is expected to be available (0 if allowed).

Because all state is in **Redis** and **PostgreSQL**, multiple instances of the application can run behind a load balancer and share the same rate limiting behavior (**distributed-safe, stateless app instances**).

### APIs

#### 1. Check rate limit

- **Endpoint**: `POST /api/v1/rate-limit/check`
- **Request body**:

```json
{
  "userId": "string",
  "endpoint": "string"
}
```

- **Response body**:

```json
{
  "allowed": true,
  "remainingTokens": 42,
  "retryAfterMs": 0
}
```

#### 2. Admin: update plan rate limit

- **Endpoint**: `PUT /api/v1/admin/plan-rate-limit`
- **Request body**:

```json
{
  "plan": "NORMAL",
  "endpoint": "/api/v1/some-endpoint",
  "capacity": 100,
  "refillRate": 10
}
```

Where:
- `plan` – `"NORMAL"` or `"PREMIUM"`.
- `capacity` – max tokens for that `(plan, endpoint)`.
- `refillRate` – tokens per minute.

### Local setup

#### Prerequisites

- Java 17
- Maven
- Docker (for Redis and PostgreSQL)

#### Start PostgreSQL (Docker)

```bash
docker run --name ratelimiter-postgres -e POSTGRES_USER=ratelimiter -e POSTGRES_PASSWORD=ratelimiter -e POSTGRES_DB=ratelimiter -p 5432:5432 -d postgres:16
```

#### Start Redis (Docker)

```bash
docker run --name ratelimiter-redis -p 6379:6379 -d redis:7
```

#### Build and run the application

```bash
mvn clean package
mvn spring-boot:run
```

Application will start on `http://localhost:8080`.

### Database schema (DDL example)

Below is an example schema compatible with the JPA entities:

```sql
CREATE TABLE plans (
    plan VARCHAR(32) PRIMARY KEY
);

INSERT INTO plans (plan) VALUES ('NORMAL') ON CONFLICT DO NOTHING;
INSERT INTO plans (plan) VALUES ('PREMIUM') ON CONFLICT DO NOTHING;

CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    plan VARCHAR(32) NOT NULL REFERENCES plans(plan)
);

CREATE TABLE plan_rate_limits (
    plan VARCHAR(32) NOT NULL REFERENCES plans(plan),
    endpoint VARCHAR(255) NOT NULL,
    capacity BIGINT NOT NULL,
    refill_rate BIGINT NOT NULL,
    PRIMARY KEY (plan, endpoint)
);
```

### Example data

```sql
INSERT INTO users (user_id, plan) VALUES ('user-normal-1', 'NORMAL');
INSERT INTO users (user_id, plan) VALUES ('user-premium-1', 'PREMIUM');

INSERT INTO plan_rate_limits (plan, endpoint, capacity, refill_rate)
VALUES
  ('NORMAL', '/api/v1/resource', 100, 10),
  ('PREMIUM', '/api/v1/resource', 500, 50);
```

### Notes on behavior

- **NORMAL vs PREMIUM plans**:
  - **PREMIUM** users should be configured with **higher capacity and refillRate** in the `plan_rate_limits` table.
  - The logic itself is plan-agnostic; it simply uses whatever values you configure.
- **Statelessness**:
  - No in-memory caches are used for rate limiting; all state is in Redis.
  - Application instances can be horizontally scaled without sharing any local state.
- **Failure modes**:
  - If a user cannot be found in PostgreSQL, `404` is returned.
  - If a rate limit is not configured for `(plan, endpoint)`, a `422` is returned.


# Milestone 4 — Inventory Service (inventory-svc)

## 1. Goal

Order-svc currently places orders without checking whether the requested products are
actually in stock. This means the system can oversell: a product with zero remaining
units can still be ordered. Milestone 4 introduces **inventory-svc**, a dedicated
microservice that:

- Tracks available stock quantity per product.
- Lets admins adjust stock levels (receiving, write-offs).
- Allows order-svc (or the gateway) to atomically reserve units before confirming an
  order, and to release them if the order is cancelled.
- Provides a single source of truth for stock, keeping product-svc focused on catalog
  data only.

---

## 2. Architecture

```
Browser / Client
      |
   Caddy (/api)
      |
   gateway:8080  ──JWT filter on mutating routes──►
      |
  ┌───┴──────────────────────────────────────────┐
  │                                              │
inventory-svc:8084                         (existing services)
      |
 PostgreSQL (inventory schema, Flyway-managed)
```

- inventory-svc is a new Spring Boot service, containerised with its own Dockerfile.
- It joins the existing `stacknet` Docker network and is not exposed publicly.
- The gateway routes `GET /api/inventory/**` without authentication and all mutating
  paths (`POST /api/inventory/**`) with the existing `JwtAuth` filter.
- inventory-svc does not call any other service directly. order-svc calls
  inventory-svc via the gateway or directly on the internal network (TBD in
  implementation; internal network preferred to avoid double JWT round-trips).
- No Redis caching in this milestone — stock counts change frequently and stale reads
  would cause reservation failures.

---

## 3. API Specification

All paths are relative to the service root (`http://inventory-svc:8084`).
The gateway exposes them under `/api/inventory/**`.

| Method | Path                              | Auth Required | Request Body                   | Success Response                          | Error Responses                        |
|--------|-----------------------------------|---------------|--------------------------------|-------------------------------------------|----------------------------------------|
| GET    | /inventory                        | None          | —                              | 200 JSON array of InventoryItem           | —                                      |
| GET    | /inventory/{productId}            | None          | —                              | 200 JSON InventoryItem                    | 404 if productId not registered        |
| POST   | /inventory/{productId}/adjust     | JWT (admin)   | `{"delta": <int>}`             | 200 JSON updated InventoryItem            | 400 if delta = 0; 404 if not found; 409 if result would go below 0 |
| POST   | /inventory/{productId}/reserve    | JWT           | `{"quantity": <positive int>}` | 200 JSON updated InventoryItem            | 400 if quantity <= 0; 404 if not found; 409 if available_qty < quantity |
| POST   | /inventory/{productId}/release    | JWT           | `{"quantity": <positive int>}` | 200 JSON updated InventoryItem            | 400 if quantity <= 0; 404 if not found; 409 if reserved_qty < quantity |
| GET    | /actuator/health                  | None          | —                              | 200 `{"status":"UP"}`                     | —                                      |

### InventoryItem response shape

```json
{
  "productId": "uuid",
  "availableQty": 42,
  "reservedQty": 3,
  "updatedAt": "2026-06-18T10:00:00Z"
}
```

`availableQty` is the quantity free for new reservations.
`reservedQty` is the quantity held by in-progress orders but not yet fulfilled.
Total physical stock = `availableQty + reservedQty`.

### Adjust semantics

`delta` may be positive (stock received) or negative (write-off / correction). A
negative delta that would reduce `available_qty` below zero is rejected with HTTP 409.

---

## 4. Data Model

Flyway migration: `V1__create_inventory.sql` in `inventory-svc/src/main/resources/db/migration/`.

### Table: `inventory`

| Column          | Type           | Constraints                                      | Notes                                     |
|-----------------|----------------|--------------------------------------------------|-------------------------------------------|
| `product_id`    | VARCHAR(36)    | PRIMARY KEY                                      | UUID; references the product catalog      |
| `available_qty` | INTEGER        | NOT NULL, DEFAULT 0, CHECK (available_qty >= 0)  | Units available for reservation           |
| `reserved_qty`  | INTEGER        | NOT NULL, DEFAULT 0, CHECK (reserved_qty >= 0)   | Units held by in-progress orders          |
| `updated_at`    | TIMESTAMP      | NOT NULL, DEFAULT NOW()                          | Updated on every write via app or trigger |

No foreign key to product-svc (cross-service FK not enforced at DB level; validated
at application layer if needed).

---

## 5. User Stories & Acceptance Criteria

### User Stories

**US-1 — View all stock levels**
As a store operator, I want to see the stock level for every product in one call, so I
can quickly spot items running low.

**US-2 — View stock for a single product**
As a frontend component or another service, I want to query stock for one product by
ID, so I can display availability to shoppers without fetching the full catalog.

**US-3 — Adjust stock**
As a warehouse admin (authenticated), I want to add or subtract units from a product's
available stock, so I can record deliveries and write-offs.

**US-4 — Reserve stock on order placement**
As order-svc, I want to atomically reserve N units when an order is placed, so that
two concurrent orders cannot both succeed when only one unit remains.

**US-5 — Release reserved stock on order cancellation**
As order-svc, I want to release previously reserved units back to available when an
order is cancelled, so stock is not permanently locked.

### Acceptance Criteria

1. `GET /inventory` returns HTTP 200 with a JSON array; the array is empty when no
   records exist.
2. `GET /inventory/{productId}` returns HTTP 200 and the correct `availableQty` and
   `reservedQty` for a known product; returns HTTP 404 for an unregistered productId.
3. `POST /inventory/{productId}/adjust` with `{"delta": 10}` increments
   `available_qty` by 10 and returns the updated record.
4. `POST /inventory/{productId}/adjust` with a negative delta that would make
   `available_qty` negative returns HTTP 409 and leaves the row unchanged.
5. `POST /inventory/{productId}/adjust` without a valid JWT returns HTTP 401.
6. `POST /inventory/{productId}/reserve` with `{"quantity": 5}` decrements
   `available_qty` by 5, increments `reserved_qty` by 5, and returns HTTP 200.
7. `POST /inventory/{productId}/reserve` when `available_qty < requested quantity`
   returns HTTP 409 and leaves both columns unchanged.
8. Two concurrent reserve requests for the last unit of a product result in exactly
   one success (HTTP 200) and one failure (HTTP 409); the final `available_qty` is 0
   and `reserved_qty` is 1.
9. `POST /inventory/{productId}/release` with `{"quantity": 5}` increments
   `available_qty` by 5, decrements `reserved_qty` by 5, and returns HTTP 200.
10. `POST /inventory/{productId}/release` when `reserved_qty < requested quantity`
    returns HTTP 409 and leaves both columns unchanged.
11. `GET /actuator/health` returns HTTP 200 `{"status":"UP"}` when the service and
    database connection are healthy.
12. The gateway routes `GET /api/inventory/**` without requiring a JWT and routes
    `POST /api/inventory/**` requiring a valid JWT (returns 401 if absent).
13. All endpoints return `Content-Type: application/json`.
14. Service starts cleanly via `docker compose up` and Flyway applies the migration
    without errors.
15. Unit tests cover: adjust with positive delta, adjust that violates the floor,
    reserve success, reserve with insufficient stock, release success, release
    over-release.

---

## 6. Out of Scope

- **Multi-location / warehouse tracking.** A single pool of stock per product. No
  concept of warehouse bins, locations, or zones.
- **SKU / variant-level tracking.** Stock is keyed by `productId` only; product
  variants (size, colour) are not modelled.
- **Automatic reservation expiry.** Reserved units that are never released (e.g., due
  to a crashed order-svc) are not auto-expired in this milestone.
- **RabbitMQ event consumption.** inventory-svc does not listen to `order.placed`
  events to trigger reservations automatically; reservations are driven by explicit
  REST calls.
- **Real-time push / WebSocket updates** for low-stock alerts.
- **Reorder / purchase-order workflows.**
- **Audit log / stock movement history.** Only the current state is stored.
- **Admin UI.** Management is via direct API calls only.

---

## 7. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| **Race condition on reserve** — two requests read the same `available_qty` before either writes, both succeed, stock goes negative. | High | High | Use a `SELECT ... FOR UPDATE` (pessimistic lock) or a single `UPDATE inventory SET available_qty = available_qty - ? WHERE product_id = ? AND available_qty >= ?` with row-count check. The database constraint `CHECK (available_qty >= 0)` acts as a final safety net. |
| **Reserved stock never released** — order-svc crashes after reserve but before confirming or cancelling. | Medium | Medium | Out of scope for M4, but design the release endpoint to be idempotent. In a future milestone, add a scheduled job or saga compensator that releases reservations older than N minutes. |
| **productId not in inventory** — order-svc tries to reserve for a product that was never seeded in inventory. | Medium | High | `POST /reserve` returns 404 for unknown productIds. Either seed inventory when a product is created (future), or document that operators must call `POST /adjust` to register a product before orders can be placed. |
| **Flyway conflicts** — migration version numbering clashes if two developers create V1 at the same time. | Low | Low | Establish naming convention: V{n}__{description}.sql; code-review gates on migration files. |
| **Gateway misconfiguration** — mutating inventory endpoints accidentally left open without JWT. | Low | High | Integration test asserts that `POST /api/inventory/**` without a token returns 401 from the gateway. |

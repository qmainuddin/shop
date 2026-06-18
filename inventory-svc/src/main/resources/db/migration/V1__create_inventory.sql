-- =============================================================
-- inventory-svc  V1  –  inventory table + seed data
-- =============================================================

-- ------------------------------------------------------------
-- Table: inventory
--
-- One row per product.  product_id carries a logical FK to
-- product-svc; no hard database-level constraint is added
-- because product-svc owns that table in a separate service.
-- reserved_quantity tracks units locked by unconfirmed orders;
-- available_quantity is the freely-purchasable stock.
-- ------------------------------------------------------------
CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL,
    available_quantity  INT             NOT NULL DEFAULT 0
                            CHECK (available_quantity >= 0),
    reserved_quantity   INT             NOT NULL DEFAULT 0
                            CHECK (reserved_quantity >= 0),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Enforce one inventory row per product.
-- Speeds up the most common read: "how many of product X are available?"
-- (point-lookup by product_id in the WHERE clause).
CREATE UNIQUE INDEX uq_inventory_product_id
    ON inventory (product_id);

-- Partial index on rows with low stock (available_quantity < 10).
-- Makes "find items about to run out" dashboard queries fast
-- without scanning the full table.
CREATE INDEX idx_inventory_low_stock
    ON inventory (product_id)
    WHERE available_quantity < 10;

-- Composite index supporting queries that filter by reserved
-- quantity and order by update recency, e.g. pending-reservation
-- reconciliation jobs.
CREATE INDEX idx_inventory_reserved_updated
    ON inventory (reserved_quantity, updated_at DESC)
    WHERE reserved_quantity > 0;

-- ------------------------------------------------------------
-- Seed data – 10 products matching product-svc seed IDs 1–10
-- ------------------------------------------------------------
INSERT INTO inventory (product_id, available_quantity, reserved_quantity) VALUES
    (1,  150, 5),
    (2,  200, 0),
    (3,   75, 10),
    (4,  120, 3),
    (5,   50, 0),
    (6,  180, 8),
    (7,   90, 2),
    (8,  160, 0),
    (9,   65, 12),
    (10, 130, 4);

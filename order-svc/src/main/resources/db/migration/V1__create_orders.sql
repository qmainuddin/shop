CREATE TABLE orders (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    total_amount NUMERIC(12,2) NOT NULL,
    placed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id          VARCHAR(36) PRIMARY KEY,
    order_id    VARCHAR(36) NOT NULL REFERENCES orders(id),
    product_id  VARCHAR(36) NOT NULL,
    quantity    INT NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL
);

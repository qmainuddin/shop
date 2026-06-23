CREATE TABLE payments (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'USD',
    provider    VARCHAR(20)   NOT NULL,   -- STRIPE | PAYPAL
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING | SUCCESS | FAILED
    provider_tx_id VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

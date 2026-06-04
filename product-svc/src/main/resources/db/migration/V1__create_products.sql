CREATE TABLE products (
    id             VARCHAR(36)     NOT NULL PRIMARY KEY,
    name           VARCHAR(255)    NOT NULL,
    description    VARCHAR(1000),
    price          NUMERIC(10, 2)  NOT NULL,
    stock_quantity INTEGER         NOT NULL DEFAULT 0,
    image_url      VARCHAR(500),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

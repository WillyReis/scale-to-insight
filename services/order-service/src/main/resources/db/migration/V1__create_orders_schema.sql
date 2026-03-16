-- Flyway migration V1: Create orders schema

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    order_code      VARCHAR(50)  NOT NULL UNIQUE,
    customer_id     VARCHAR(50)  NOT NULL,
    channel         VARCHAR(50)  NOT NULL DEFAULT 'WEB',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  VARCHAR(50)  NOT NULL,
    product_name VARCHAR(255),
    quantity    INT          NOT NULL DEFAULT 1,
    unit_price  NUMERIC(12,2) NOT NULL,
    total_price NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

CREATE INDEX IF NOT EXISTS idx_orders_customer    ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at  ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order  ON order_items(order_id);

-- Flyway migration V1: Create products schema

CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL PRIMARY KEY,
    product_code    VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    sub_category    VARCHAR(100),
    sku             VARCHAR(100) UNIQUE,
    unit_cost       NUMERIC(12,2),
    unit_price      NUMERIC(12,2) NOT NULL,
    stock_quantity  INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_active   ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_sku      ON products(sku);

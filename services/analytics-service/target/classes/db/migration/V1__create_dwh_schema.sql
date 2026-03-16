-- Flyway migration V1: Create DWH schema (analytics-service view of the DWH)
-- The full DWH schema is initialized by the postgres init script.
-- This migration creates the same tables so Flyway is aware of schema state.

-- Dimension: Date
CREATE TABLE IF NOT EXISTS dim_date (
    date_key        SERIAL PRIMARY KEY,
    full_date       DATE        NOT NULL UNIQUE,
    day_of_week     SMALLINT    NOT NULL,
    day_name        VARCHAR(10) NOT NULL,
    month_number    SMALLINT    NOT NULL,
    month_name      VARCHAR(10) NOT NULL,
    quarter         SMALLINT    NOT NULL,
    year            SMALLINT    NOT NULL,
    is_weekend      BOOLEAN     NOT NULL DEFAULT FALSE,
    is_holiday      BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Dimension: Product
CREATE TABLE IF NOT EXISTS dim_product (
    product_key     SERIAL PRIMARY KEY,
    product_id      VARCHAR(50) NOT NULL UNIQUE,
    product_name    VARCHAR(255) NOT NULL,
    category        VARCHAR(100),
    sub_category    VARCHAR(100),
    unit_cost       NUMERIC(12,2),
    unit_price      NUMERIC(12,2),
    sku             VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from      TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_to        TIMESTAMP
);

-- Dimension: Customer
CREATE TABLE IF NOT EXISTS dim_customer (
    customer_key    SERIAL PRIMARY KEY,
    customer_id     VARCHAR(50) NOT NULL UNIQUE,
    full_name       VARCHAR(255),
    email           VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(50),
    country         VARCHAR(100) DEFAULT 'Brazil',
    segment         VARCHAR(50),
    valid_from      TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_to        TIMESTAMP
);

-- Dimension: Channel
CREATE TABLE IF NOT EXISTS dim_channel (
    channel_key     SERIAL PRIMARY KEY,
    channel_code    VARCHAR(50) NOT NULL UNIQUE,
    channel_name    VARCHAR(100) NOT NULL,
    channel_type    VARCHAR(50)
);

-- Fact: Sales
CREATE TABLE IF NOT EXISTS fact_sales (
    sales_key       BIGSERIAL PRIMARY KEY,
    order_id        VARCHAR(50)  NOT NULL,
    date_key        INT          NOT NULL REFERENCES dim_date(date_key),
    product_key     INT          NOT NULL REFERENCES dim_product(product_key),
    customer_key    INT          NOT NULL REFERENCES dim_customer(customer_key),
    channel_key     INT          NOT NULL REFERENCES dim_channel(channel_key),
    quantity        INT          NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12,2) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    gross_revenue   NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    net_revenue     NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price - discount_amount) STORED,
    cost_of_goods   NUMERIC(12,2),
    gross_profit    NUMERIC(12,2) GENERATED ALWAYS AS
                        (quantity * unit_price - discount_amount - COALESCE(cost_of_goods, 0)) STORED,
    loaded_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Fact: Financial Health
CREATE TABLE IF NOT EXISTS fact_financial_health (
    financial_key   BIGSERIAL PRIMARY KEY,
    date_key        INT          NOT NULL REFERENCES dim_date(date_key),
    channel_key     INT          NOT NULL REFERENCES dim_channel(channel_key),
    total_orders    INT          NOT NULL DEFAULT 0,
    total_revenue   NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_cost      NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_profit    NUMERIC(14,2) NOT NULL DEFAULT 0,
    profit_margin   NUMERIC(6,4),
    loaded_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_fact_sales_date     ON fact_sales(date_key);
CREATE INDEX IF NOT EXISTS idx_fact_sales_product  ON fact_sales(product_key);
CREATE INDEX IF NOT EXISTS idx_fact_sales_customer ON fact_sales(customer_key);
CREATE INDEX IF NOT EXISTS idx_fact_sales_channel  ON fact_sales(channel_key);
CREATE INDEX IF NOT EXISTS idx_fin_health_date     ON fact_financial_health(date_key);

-- Seed channels
INSERT INTO dim_channel (channel_code, channel_name, channel_type) VALUES
    ('WEB',         'Website',       'Online'),
    ('MOBILE',      'Mobile App',    'Online'),
    ('MARKETPLACE', 'Marketplace',   'Online'),
    ('STORE',       'Physical Store','Offline')
ON CONFLICT (channel_code) DO NOTHING;

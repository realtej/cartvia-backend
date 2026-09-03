-- CartVia core schema

CREATE TABLE stores (
    id              CHAR(36) PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    address         VARCHAR(500),
    timezone        VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE TABLE users (
    id              CHAR(36) PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150),
    phone           VARCHAR(15),
    password_hash   VARCHAR(100) NOT NULL,
    gender          VARCHAR(10),
    role            VARCHAR(20)  NOT NULL,
    store_id        CHAR(36) REFERENCES stores (id),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
);

CREATE INDEX idx_users_store_id ON users (store_id);

CREATE TABLE refresh_tokens (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36)         NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

CREATE TABLE password_reset_tokens (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36)         NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_password_reset_tokens_token_hash ON password_reset_tokens (token_hash);

CREATE TABLE products (
    id                    CHAR(36) PRIMARY KEY,
    barcode               VARCHAR(50)   NOT NULL,
    name                  VARCHAR(200)  NOT NULL,
    description           VARCHAR(1000),
    category              VARCHAR(100),
    price                 NUMERIC(12,2) NOT NULL,
    discount_pct          NUMERIC(5,2)  NOT NULL DEFAULT 0,
    expected_weight_g     NUMERIC(12,2),
    weight_tolerance_pct  NUMERIC(5,2),
    gst_slab_pct          NUMERIC(5,2),
    image_url             VARCHAR(500),
    store_id              CHAR(36) REFERENCES stores (id),
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    CONSTRAINT uk_products_barcode UNIQUE (barcode)
);

CREATE INDEX idx_products_store_id ON products (store_id);
CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_name ON products (name);

CREATE TABLE inventory (
    id                  CHAR(36) PRIMARY KEY,
    store_id            CHAR(36) NOT NULL REFERENCES stores (id),
    product_id          CHAR(36) NOT NULL REFERENCES products (id),
    stock_qty           INT  NOT NULL DEFAULT 0,
    low_stock_threshold INT  NOT NULL DEFAULT 10,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT uk_inventory_store_product UNIQUE (store_id, product_id)
);

CREATE INDEX idx_inventory_store_id ON inventory (store_id);

CREATE TABLE trolleys (
    id                CHAR(36) PRIMARY KEY,
    trolley_code      VARCHAR(50)  NOT NULL,
    store_id          CHAR(36) REFERENCES stores (id),
    esp32_mac         VARCHAR(17),
    status            VARCHAR(20)  NOT NULL,
    device_token_hash VARCHAR(128),
    last_seen_at      TIMESTAMP,
    battery_pct       INT,
    rssi              INT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uk_trolleys_code UNIQUE (trolley_code)
);

CREATE INDEX idx_trolleys_store_id ON trolleys (store_id);
CREATE INDEX idx_trolleys_device_token_hash ON trolleys (device_token_hash);

CREATE TABLE shopping_sessions (
    id            CHAR(36) PRIMARY KEY,
    session_code  VARCHAR(50) NOT NULL,
    user_id       CHAR(36)        NOT NULL REFERENCES users (id),
    trolley_id    CHAR(36)        NOT NULL REFERENCES trolleys (id),
    store_id      CHAR(36)        NOT NULL REFERENCES stores (id),
    status        VARCHAR(20) NOT NULL,
    started_at    TIMESTAMP   NOT NULL,
    ended_at      TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    CONSTRAINT uk_sessions_code UNIQUE (session_code)
);

CREATE INDEX idx_sessions_user_id ON shopping_sessions (user_id);
CREATE INDEX idx_sessions_trolley_status ON shopping_sessions (trolley_id, status);
CREATE INDEX idx_sessions_store_id ON shopping_sessions (store_id);

CREATE TABLE carts (
    id         CHAR(36) PRIMARY KEY,
    session_id CHAR(36) UNIQUE REFERENCES shopping_sessions (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE cart_items (
    id                   CHAR(36) PRIMARY KEY,
    cart_id              CHAR(36)          NOT NULL REFERENCES carts (id),
    product_id           CHAR(36)          NOT NULL REFERENCES products (id),
    quantity             INT           NOT NULL DEFAULT 1,
    unit_price           NUMERIC(12,2) NOT NULL,
    source               VARCHAR(10),
    verification_status  VARCHAR(20)   NOT NULL,
    measured_weight_g    NUMERIC(12,2),
    client_ts            TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

CREATE TABLE orders (
    id              CHAR(36) PRIMARY KEY,
    order_code      VARCHAR(50)   NOT NULL,
    user_id         CHAR(36)          NOT NULL REFERENCES users (id),
    session_id      CHAR(36) REFERENCES shopping_sessions (id),
    store_id        CHAR(36)          NOT NULL REFERENCES stores (id),
    status          VARCHAR(20)   NOT NULL,
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_total  NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    grand_total     NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uk_orders_code UNIQUE (order_code)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_store_status ON orders (store_id, status);

CREATE TABLE order_items (
    id          CHAR(36) PRIMARY KEY,
    order_id    CHAR(36)          NOT NULL REFERENCES orders (id),
    product_id  CHAR(36)          NOT NULL REFERENCES products (id),
    name        VARCHAR(200)  NOT NULL,
    quantity    INT           NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL,
    line_total  NUMERIC(12,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payments (
    id               CHAR(36) PRIMARY KEY,
    order_id         CHAR(36)          NOT NULL REFERENCES orders (id),
    amount           NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    upi_txn_ref      VARCHAR(100),
    gateway_ref      VARCHAR(100),
    idempotency_key  VARCHAR(128),
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE UNIQUE INDEX idx_payments_idempotency_key ON payments (idempotency_key);

CREATE TABLE webhook_events (
    id           CHAR(36) PRIMARY KEY,
    payment_id   CHAR(36) REFERENCES payments (id),
    external_id  VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_webhook_events_external_id UNIQUE (external_id)
);

CREATE TABLE offers (
    id           CHAR(36) PRIMARY KEY,
    store_id     CHAR(36)          NOT NULL REFERENCES stores (id),
    product_id   CHAR(36) REFERENCES products (id),
    title        VARCHAR(150)  NOT NULL,
    discount_pct NUMERIC(5,2)  NOT NULL,
    valid_from   TIMESTAMP,
    valid_to     TIMESTAMP,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);

CREATE INDEX idx_offers_store_id ON offers (store_id);
CREATE INDEX idx_offers_product_id ON offers (product_id);

CREATE TABLE notifications (
    id         CHAR(36) PRIMARY KEY,
    user_id    CHAR(36)          NOT NULL REFERENCES users (id),
    type       VARCHAR(50)   NOT NULL,
    title      VARCHAR(200)  NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    read_flag  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_flag);

CREATE TABLE shopping_lists (
    id         CHAR(36) PRIMARY KEY,
    user_id    CHAR(36)         NOT NULL REFERENCES users (id),
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_shopping_lists_user_id ON shopping_lists (user_id);

CREATE TABLE shopping_list_items (
    id           CHAR(36) PRIMARY KEY,
    list_id      CHAR(36)         NOT NULL REFERENCES shopping_lists (id),
    product_id   CHAR(36) REFERENCES products (id),
    custom_name  VARCHAR(200),
    purchased    BOOLEAN      NOT NULL DEFAULT FALSE,
    purchased_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_shopping_list_items_list_id ON shopping_list_items (list_id);

CREATE TABLE recommendation_entries (
    id                 CHAR(36) PRIMARY KEY,
    type               VARCHAR(40) NOT NULL,
    user_id            CHAR(36) REFERENCES users (id),
    product_id         CHAR(36)        NOT NULL REFERENCES products (id),
    related_product_id CHAR(36) REFERENCES products (id),
    category           VARCHAR(100),
    score              DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at         TIMESTAMP   NOT NULL,
    updated_at         TIMESTAMP   NOT NULL
);

CREATE INDEX idx_recommendations_user_id ON recommendation_entries (user_id);
CREATE INDEX idx_recommendations_product_id ON recommendation_entries (product_id);
CREATE INDEX idx_recommendations_category ON recommendation_entries (category);

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','STAFF','ADMIN')),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE categories (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(80) NOT NULL UNIQUE,
    description   VARCHAR(255),
    image_url     VARCHAR(500),
    display_order INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE food_items (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT        NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    name             VARCHAR(120)  NOT NULL,
    description      VARCHAR(500),
    price            NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    special_price    NUMERIC(10,2),
    is_today_special BOOLEAN       NOT NULL DEFAULT FALSE,
    availability     BOOLEAN       NOT NULL DEFAULT TRUE,
    image_url        VARCHAR(500),
    prep_time_min    INT           NOT NULL DEFAULT 10,
    is_veg           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_food_items_category ON food_items(category_id);
CREATE INDEX idx_food_items_avail ON food_items(availability);

CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount   NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
    status         VARCHAR(20)   NOT NULL CHECK (status IN ('PLACED','ACCEPTED','PREPARING','READY','COMPLETED','CANCELLED')),
    token_number   VARCHAR(20)   NOT NULL UNIQUE,
    payment_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    notes          VARCHAR(500),
    order_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_date ON orders(order_date);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    food_item_id BIGINT        NOT NULL REFERENCES food_items(id) ON DELETE RESTRICT,
    quantity     INT           NOT NULL CHECK (quantity > 0),
    price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    subtotal     NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0)
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_food ON order_items(food_item_id);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    amount              NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    payment_method      VARCHAR(40)   NOT NULL,
    payment_status      VARCHAR(20)   NOT NULL CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    transaction_id      VARCHAR(120),
    razorpay_order_id   VARCHAR(120),
    razorpay_payment_id VARCHAR(120),
    razorpay_signature  VARCHAR(255),
    payment_date        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_item_id BIGINT    NOT NULL REFERENCES food_items(id) ON DELETE CASCADE,
    order_id     BIGINT    NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rating       INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_food_review UNIQUE (user_id, food_item_id, order_id)
);
CREATE INDEX idx_reviews_food ON reviews(food_item_id);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

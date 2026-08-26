-- =====================================================
-- Smart Canteen Management System
-- PostgreSQL Schema (for production deployment)
-- =====================================================

-- Drop in reverse-dependency order
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS food_items CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ===================== USERS =====================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password        VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)   NOT NULL CHECK (role IN ('STUDENT','STAFF','ADMIN')),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_role ON users(role);

-- ===================== CATEGORIES =====================
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(80)   NOT NULL UNIQUE,
    description     VARCHAR(255),
    image_url       VARCHAR(500),
    display_order   INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================== FOOD ITEMS =====================
CREATE TABLE food_items (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT        NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    name            VARCHAR(120)  NOT NULL,
    description     VARCHAR(500),
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    availability    BOOLEAN       NOT NULL DEFAULT TRUE,
    image_url       VARCHAR(500),
    prep_time_min   INT           NOT NULL DEFAULT 10,
    is_veg          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_food_items_category ON food_items(category_id);
CREATE INDEX idx_food_items_avail ON food_items(availability);

-- ===================== ORDERS =====================
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount    NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
    status          VARCHAR(20)   NOT NULL CHECK (status IN ('PLACED','ACCEPTED','PREPARING','READY','COMPLETED','CANCELLED')),
    token_number    VARCHAR(20)   NOT NULL UNIQUE,
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    notes           VARCHAR(500),
    order_date      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_date ON orders(order_date);

-- ===================== ORDER ITEMS =====================
CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    food_item_id    BIGINT        NOT NULL REFERENCES food_items(id) ON DELETE RESTRICT,
    quantity        INT           NOT NULL CHECK (quantity > 0),
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    subtotal        NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0)
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_food ON order_items(food_item_id);

-- ===================== PAYMENTS =====================
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    amount          NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    payment_method  VARCHAR(40)   NOT NULL,
    payment_status  VARCHAR(20)   NOT NULL CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    transaction_id  VARCHAR(120),
    razorpay_order_id       VARCHAR(120),
    razorpay_payment_id     VARCHAR(120),
    razorpay_signature      VARCHAR(255),
    payment_date    TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================== REVIEWS =====================
CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_item_id    BIGINT        NOT NULL REFERENCES food_items(id) ON DELETE CASCADE,
    order_id        BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rating          INT           NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         VARCHAR(500),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_food_review UNIQUE (user_id, food_item_id, order_id)
);
CREATE INDEX idx_reviews_food ON reviews(food_item_id);

-- ===================== NOTIFICATIONS =====================
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(150)  NOT NULL,
    message         VARCHAR(500)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

-- =====================================================
-- SEED DATA (passwords BCrypt hashed)
-- Admin@123, Staff@123, Student@123
-- Hashes generated with strength=10
-- =====================================================

INSERT INTO users (name, email, password, phone, role) VALUES
('Admin User',    'admin@canteen.com',    '$2a$10$qE5kVJ0aO/3vFhkLm2P4ROqUkGXLpPlJcDXVy1nYHfvKKdEQmxsaC', '9999900000', 'ADMIN'),
('Staff User',    'staff@canteen.com',    '$2a$10$D1y1iRn5pVyT5jXJyOov7OhqKqpVbp1pVOQMxGGK2PNxQ2TLwlzsa', '9999900001', 'STAFF'),
('Student User',  'student@canteen.com',  '$2a$10$lD3LGCjQqW.vYxUKPL7CDeSPqRpVu6sJ6zRJx7.eRMk8LY9YHJ25.', '9999900002', 'STUDENT');

INSERT INTO categories (name, description, display_order) VALUES
('South Indian', 'Idli, Dosa, Vada and more', 1),
('North Indian', 'Curries, Roti, Paratha', 2),
('Snacks',       'Samosa, Sandwich, Pakora', 3),
('Beverages',    'Tea, Coffee, Juice', 4),
('Desserts',     'Sweet treats and ice cream', 5);

INSERT INTO food_items (category_id, name, description, price, availability, prep_time_min, is_veg) VALUES
(1, 'Masala Dosa',     'Crispy dosa with potato masala',     80.00, TRUE, 12, TRUE),
(1, 'Idli Sambar',     'Soft idlis with sambar & chutney',   50.00, TRUE, 8,  TRUE),
(1, 'Medu Vada',       'Crispy vadas with chutney',          60.00, TRUE, 10, TRUE),
(2, 'Paneer Butter Masala', 'Creamy paneer curry with roti', 150.00, TRUE, 15, TRUE),
(2, 'Veg Biryani',     'Aromatic rice with vegetables',      130.00, TRUE, 18, TRUE),
(2, 'Aloo Paratha',    'Stuffed potato paratha',             70.00, TRUE, 10, TRUE),
(3, 'Samosa',          'Crispy pastry with potato filling',   20.00, TRUE, 5,  TRUE),
(3, 'Veg Sandwich',    'Grilled sandwich with veggies',      60.00, TRUE, 7,  TRUE),
(3, 'Pakora Plate',    'Assorted vegetable pakoras',         50.00, TRUE, 8,  TRUE),
(4, 'Masala Chai',     'Indian spiced tea',                  15.00, TRUE, 3,  TRUE),
(4, 'Filter Coffee',   'South Indian filter coffee',         25.00, TRUE, 4,  TRUE),
(4, 'Fresh Lime Soda', 'Refreshing lime soda',               35.00, TRUE, 3,  TRUE),
(5, 'Gulab Jamun',     'Soft milk-solid dumplings in syrup',  40.00, TRUE, 3,  TRUE),
(5, 'Ice Cream',       'Vanilla / Chocolate / Strawberry',    50.00, TRUE, 2,  TRUE),
(5, 'Brownie',         'Warm chocolate brownie',             70.00, TRUE, 5,  TRUE);